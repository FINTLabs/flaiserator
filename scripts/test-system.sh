#!/usr/bin/env bash
set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repository_root"

for command in docker kind helm kubectl java; do
    command -v "$command" >/dev/null 2>&1 || {
        printf 'Required command not found: %s\n' "$command" >&2
        exit 1
    }
done
docker info >/dev/null

workdir="$(mktemp -d "${TMPDIR:-/tmp}/flaiserator-system-test.XXXXXX")"
cluster_name="flaiserator-test-$(date +%s)-$$"
timeout="${SYSTEM_TEST_TIMEOUT:-180s}"
artifacts="${SYSTEM_TEST_ARTIFACTS:-${repository_root}/build/system-test}"
operator_namespace="flais-system"
test_namespace="system-test"
cluster_created=false
export KUBECONFIG="${workdir}/kubeconfig"
export KIND_EXPERIMENTAL_PROVIDER=docker

kube() {
    kubectl --request-timeout=30s "$@"
}

cleanup() {
    local result=$?
    trap - EXIT
    set +e
    if [[ "$cluster_created" == true ]]; then
        mkdir -p "$artifacts"
        kind export logs --name "$cluster_name" "${artifacts}/kind" >"${artifacts}/kind-export.log" 2>&1
        kube get pods -A -o wide >"${artifacts}/pods.txt" 2>&1
        kube get events -A --sort-by=.lastTimestamp >"${artifacts}/events.txt" 2>&1
        kube -n "$operator_namespace" logs deployment/flaiserator --all-containers --tail=1000 >"${artifacts}/operator.log" 2>&1
        kube -n "$operator_namespace" describe pods >"${artifacts}/operator-pods.txt" 2>&1
        kube -n "$test_namespace" get applications.fintlabs.no,jobs.fintlabs.no -o yaml >"${artifacts}/resources.yaml" 2>&1
        kube -n "$test_namespace" describe pods >"${artifacts}/workload-pods.txt" 2>&1
        if (( result != 0 )); then
            cat "${artifacts}/operator.log" "${artifacts}/workload-pods.txt" >&2
        fi
        if [[ "${KEEP_CLUSTER:-false}" == true ]]; then
            printf 'Cluster kept: %s\nKubeconfig: %s\n' "$cluster_name" "$KUBECONFIG"
        else
            kind delete cluster --name "$cluster_name"
            rm -rf -- "$workdir"
        fi
        printf 'System test diagnostics: %s\n' "$artifacts"
    else
        rm -rf -- "$workdir"
    fi
    exit "$result"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

assert_eq() {
    if [[ "$1" != "$2" ]]; then
        printf 'Assertion failed (%s): expected "%s", got "%s"\n' "$3" "$2" "$1" >&2
        exit 1
    fi
}

printf 'Building the operator and test workloads...\n'
./gradlew --no-daemon clean assemble
docker build -t flaiserator:system-test .
docker build --target application -t flaiserator-test-application:system-test tests/system
docker build --target job -t flaiserator-test-job:system-test tests/system

printf 'Creating isolated Kind cluster %s...\n' "$cluster_name"
cluster_created=true
kind create cluster --name "$cluster_name" --kubeconfig "$KUBECONFIG" \
    --image "${KIND_NODE_IMAGE:-kindest/node:v1.35.0}" --wait "$timeout"
kind load docker-image --name "$cluster_name" \
    flaiserator:system-test flaiserator-test-application:system-test flaiserator-test-job:system-test

# The operator watches these APIs even when the associated integrations are unused.
# Only their real CRD definitions are needed; no external controllers are installed.
kube apply --server-side -f src/main/resources/kubernetes
kube wait --for=condition=Established crd --all --timeout="$timeout"

cp -R charts/flaiserator-crd "${workdir}/flaiserator-crd"
helm dependency build "${workdir}/flaiserator-crd" --skip-refresh
helm install flaiserator-crd "${workdir}/flaiserator-crd" \
    --namespace "$operator_namespace" --create-namespace --wait --timeout "$timeout"
kube wait --for=condition=Established \
    crd/applications.fintlabs.no crd/jobs.fintlabs.no --timeout="$timeout"
helm install flaiserator charts/flaiserator \
    --namespace "$operator_namespace" --wait --timeout "$timeout" \
    --set image.repository=flaiserator --set image.tag=system-test --set image.pullPolicy=Never
kube -n "$operator_namespace" rollout status deployment/flaiserator --timeout="$timeout"
assert_eq "$(kube -n "$operator_namespace" get deployment flaiserator -o jsonpath='{.spec.template.spec.containers[0].image}')" \
    'flaiserator:system-test' 'Helm uses the locally built Docker image'

printf 'Testing Application reconciliation and HTTP through its Service...\n'
kube create namespace "$test_namespace"
kube -n "$test_namespace" apply -f tests/system/application.yaml
kube -n "$test_namespace" wait --for=jsonpath='{.status.state}'=DEPLOYED \
    applications.fintlabs.no/system-test-app --timeout="$timeout"
kube -n "$test_namespace" wait --for=create deployment/system-test-app --timeout="$timeout"
kube -n "$test_namespace" rollout status deployment/system-test-app --timeout="$timeout"
kube -n "$test_namespace" wait --for=create service/system-test-app --timeout="$timeout"
assert_eq "$(kube -n "$test_namespace" exec deployment/system-test-app -- \
    wget -T 10 -qO- http://system-test-app:8080)" 'flaiserator-system-test' 'Application HTTP response'

printf 'Testing Job reconciliation and successful execution...\n'
kube -n "$test_namespace" apply -f tests/system/job.yaml
kube -n "$test_namespace" wait --for=create jobs.batch/system-test-job-1 --timeout="$timeout"
kube -n "$test_namespace" wait --for=condition=Complete \
    jobs.batch/system-test-job-1 --timeout="$timeout"
kube -n "$test_namespace" wait --for=jsonpath='{.status.state}'=DEPLOYED \
    jobs.fintlabs.no/system-test-job --timeout="$timeout"
assert_eq "$(kube -n "$test_namespace" logs jobs.batch/system-test-job-1)" \
    'flaiserator-system-test-job-completed' 'Job output'
assert_eq "$(kube -n "$test_namespace" get cronjob system-test-job -o jsonpath='{.spec.suspend}')" \
    'true' 'One-shot Job has a suspended CronJob'

printf 'Testing garbage collection after custom resource deletion...\n'
kube -n "$test_namespace" delete -f tests/system/application.yaml -f tests/system/job.yaml --timeout="$timeout"
kube -n "$test_namespace" wait --for=delete deployment/system-test-app service/system-test-app \
    cronjob/system-test-job jobs.batch/system-test-job-1 --timeout="$timeout"
printf 'System tests passed.\n'
