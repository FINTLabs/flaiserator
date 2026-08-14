#!/usr/bin/env bash
set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
workdir="$(mktemp -d "${TMPDIR:-/tmp}/flaiserator-helm-test.XXXXXX")"

cleanup() {
    rm -rf -- "$workdir"
}
trap cleanup EXIT

readonly test_chart_version="0.1.0"
readonly test_app_version="20240101-abcdef0"
readonly release_name="flaiserator"
readonly namespace="flais-system"

fail() {
    printf 'ERROR: %s\n' "$*" >&2
    exit 1
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || fail "Required command '$1' was not found."
}

assert_eq() {
    local actual="$1"
    local expected="$2"
    local message="$3"

    if [[ "$actual" != "$expected" ]]; then
        fail "${message}: expected '${expected}', got '${actual}'"
    fi
}

yq_query() {
    local file="$1"
    local query="$2"

    yq ea -r "$query" "$file"
}

assert_query_eq() {
    local file="$1"
    local query="$2"
    local expected="$3"
    local message="$4"

    assert_eq "$(yq_query "$file" "$query")" "$expected" "$message"
}

assert_resource_count() {
    local file="$1"
    local kind="$2"
    local expected="$3"

    assert_query_eq "$file" "[select(.kind == \"${kind}\")] | length" "$expected" "Unexpected ${kind} count"
}

copy_chart() {
    local chart_name="$1"

    cp -R "${repository_root}/charts/${chart_name}" "${workdir}/${chart_name}"
    yq e -i ".version = \"${test_chart_version}\" | .appVersion = \"${test_app_version}\"" "${workdir}/${chart_name}/Chart.yaml"
}

test_flaiserator_chart_defaults() {
    local chart="${workdir}/flaiserator"
    local rendered="${workdir}/flaiserator-defaults.yaml"

    helm lint "$chart"
    helm template "$release_name" "$chart" --namespace "$namespace" > "$rendered"

    assert_query_eq "$rendered" '[.] | length' "6" "Unexpected rendered resource count"
    assert_resource_count "$rendered" "NetworkPolicy" "1"
    assert_resource_count "$rendered" "ServiceAccount" "1"
    assert_resource_count "$rendered" "ClusterRole" "1"
    assert_resource_count "$rendered" "ClusterRoleBinding" "1"
    assert_resource_count "$rendered" "Deployment" "1"
    assert_resource_count "$rendered" "PodMonitor" "1"

    assert_query_eq "$rendered" 'select(.kind == "Deployment") | .metadata.name' "$release_name" "Deployment name"
    assert_query_eq "$rendered" 'select(.kind == "Deployment") | .spec.template.spec.serviceAccountName' "$release_name" "Deployment service account"
    assert_query_eq "$rendered" 'select(.kind == "Deployment") | .spec.template.spec.containers[] | select(.name == "flaiserator") | .image' "ghcr.io/fintlabs/flaiserator:${test_app_version}" "Deployment image"
    assert_query_eq "$rendered" 'select(.kind == "ClusterRoleBinding") | .subjects[0].namespace' "$namespace" "ClusterRoleBinding subject namespace"
    assert_query_eq "$rendered" 'select(.kind == "PodMonitor") | .spec.podMetricsEndpoints[0].path' "/metrics" "PodMonitor metrics path"
    assert_query_eq "$rendered" '[select(.kind == "Deployment") | .spec.template.spec.containers[] | select(.name == "flaiserator") | .env[] | select(.name == "FLAISERATOR_OBSERVABILITY_OTEL_INSTRUMENTATION_COLLECTORENDPOINT")] | length' "0" "Instrumentation endpoint env var should be omitted by default"
}

test_flaiserator_chart_overrides() {
    local chart="${workdir}/flaiserator"
    local rendered="${workdir}/flaiserator-overrides.yaml"

    helm template "$release_name" "$chart" \
        --namespace "$namespace" \
        --set serviceAccount.create=false \
        --set serviceAccount.name=existing-flaiserator \
        --set config.ingress.traefikV3CrdSupport=true \
        --set config.observability.otel.enabled=true \
        --set config.observability.otel.autoInstrumentation.ebpfEnabled=true \
        --set config.observability.otel.autoInstrumentation.sdkInjectionEnabled=true \
        --set-string config.observability.otel.instrumentation.collectorEndpoint=http://otel-collector:4318 \
        --set-string config.observability.otel.instrumentation.collectorProtocol=http/protobuf \
        --set-string config.observability.otel.instrumentation.collectorInsecure=true \
        > "$rendered"

    assert_resource_count "$rendered" "ServiceAccount" "0"
    assert_resource_count "$rendered" "ClusterRoleBinding" "0"
    assert_query_eq "$rendered" 'select(.kind == "Deployment") | .spec.template.spec.serviceAccountName' "existing-flaiserator" "Deployment service account override"
    assert_query_eq "$rendered" 'select(.kind == "Deployment") | .spec.template.spec.containers[] | select(.name == "flaiserator") | .env[] | select(.name == "FLAISERATOR_INGRESS_TRAEFIKV3CRDSUPPORT") | .value' "true" "Traefik v3 env value"
    assert_query_eq "$rendered" 'select(.kind == "Deployment") | .spec.template.spec.containers[] | select(.name == "flaiserator") | .env[] | select(.name == "FLAISERATOR_OBSERVABILITY_OTEL_ENABLED") | .value' "true" "OTel enabled env value"
    assert_query_eq "$rendered" 'select(.kind == "Deployment") | .spec.template.spec.containers[] | select(.name == "flaiserator") | .env[] | select(.name == "FLAISERATOR_OBSERVABILITY_OTEL_INSTRUMENTATION_COLLECTORENDPOINT") | .value' "http://otel-collector:4318" "OTel collector endpoint env value"
    assert_query_eq "$rendered" 'select(.kind == "Deployment") | .spec.template.spec.containers[] | select(.name == "flaiserator") | .env[] | select(.name == "FLAISERATOR_OBSERVABILITY_OTEL_INSTRUMENTATION_COLLECTORPROTOCOL") | .value' "http/protobuf" "OTel collector protocol env value"
    assert_query_eq "$rendered" 'select(.kind == "Deployment") | .spec.template.spec.containers[] | select(.name == "flaiserator") | .env[] | select(.name == "FLAISERATOR_OBSERVABILITY_OTEL_INSTRUMENTATION_COLLECTORINSECURE") | .value' "true" "OTel collector insecure env value"
}

test_flaiserator_crd_chart() {
    local chart="${workdir}/flaiserator-crd"
    local rendered="${workdir}/flaiserator-crd.yaml"

    helm dependency build "$chart" --skip-refresh
    helm lint "$chart"
    helm template flaiserator-crd "$chart" \
        --namespace "$namespace" \
        --set-string 'crds.annotations.helm\.sh/resource-policy=keep' \
        > "$rendered"

    assert_resource_count "$rendered" "CustomResourceDefinition" "2"

    for crd_name in applications.fintlabs.no jobs.fintlabs.no; do
        assert_query_eq "$rendered" "[select(.metadata.name == \"${crd_name}\")] | length" "1" "${crd_name} CRD count"
        assert_query_eq "$rendered" "select(.metadata.name == \"${crd_name}\") | .spec.group" "fintlabs.no" "${crd_name} group"
        assert_query_eq "$rendered" "select(.metadata.name == \"${crd_name}\") | .spec.scope" "Namespaced" "${crd_name} scope"
        assert_query_eq "$rendered" "select(.metadata.name == \"${crd_name}\") | .spec.versions[] | select(.name == \"v1alpha1\") | .served" "true" "${crd_name} v1alpha1 served"
        assert_query_eq "$rendered" "select(.metadata.name == \"${crd_name}\") | .spec.versions[] | select(.name == \"v1alpha1\") | .storage" "true" "${crd_name} v1alpha1 storage"
        assert_query_eq "$rendered" "select(.metadata.name == \"${crd_name}\") | .metadata.annotations.\"helm.sh/resource-policy\"" "keep" "${crd_name} annotation"
    done
}

main() {
    require_command helm
    require_command yq

    copy_chart flaiserator
    copy_chart flaiserator-crd

    test_flaiserator_chart_defaults
    test_flaiserator_chart_overrides
    test_flaiserator_crd_chart

    printf 'Helm chart tests passed.\n'
}

main "$@"
