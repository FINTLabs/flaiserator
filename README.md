# FLAISerator
![App Build and Publish](https://github.com/FINTLabs/flaiserator/actions/workflows/app-build-publish.yaml/badge.svg) ![CodeQL](https://github.com/FINTLabs/flaiserator/actions/workflows/codeql.yaml/badge.svg)

_FLAISerator is a template engine that runs within Kubernetes_.

FLAISerator is a Kubernetes operator that handles the lifecycle of FLAIS custom resources, currently `fintlabs.no/Application`.
The main goal of FLAISerator is to simplify application deployment by providing a high-level abstraction tailored for our lightweight PaaS, 
FLAIS (FINTLabs Application Infrastructure Service).

When an `Application` resource is created in Kubernetes,
FLAISerator will generate several other Kubernetes resources that work together to form a complete deployment.
All of these resources will remain in Kubernetes, until the `Application` resource is deleted, upon which they will be removed.

## Generated resources

Kubernetes built-ins:
* `Deployment` that runs program executables,
* `IngressRoute` adding virtualhost support using Traefik,
* `Secret` for stuff that shouldn't be shared with anyone,
* `Service` which points to the application endpoint.

<!--
* `ServiceAccount` for granting correct permissions to managed resources,
* `NetworkPolicy` for firewall configuration,
* `HorizontalPodAutoscaler` for automatic application scaling,
* `VerticalPodAutoscaler` for automatic application scaling,
-->

FLAIS resources for external system provisioning:
* `AzureBlobContainer` and `AzureFileShare` for [Azurerator](https://github.com/FINTLabs/azurerator),
* `NamOAuthClientApplicationResource` for [NAMerator](https://github.com/FINTLabs/namerator),
* `PGSchemaAndUser` for [PGerator](https://github.com/FINTLabs/pgerator),
* `AivenKafkaAcl` for [Aivenerator](https://github.com/FINTLabs/aivenerator),

## Documentation

The entire specification for the manifest is documented [TODO]().

## System tests

Run `scripts/test-system.sh` from a checkout with Java 21, Docker, Kind v0.33.0,
Helm, and kubectl installed. Docker must be running. The first run downloads
Gradle dependencies and container images; allow a few minutes and enough Docker
memory for a Kubernetes node and the operator (4 GiB is a useful starting point).

The script builds the application JARs and the repository's Dockerfile, creates a
disposable Kind cluster, and installs both the CRD chart and operator chart using
Helm. It then creates a minimal `Application` and one-shot `Job`, checks the
application's readiness and HTTP response through its Service, checks successful
job completion and output, and verifies that deleting the custom resources removes
their generated workloads.

The test installs the external CRD definitions from `src/main/resources/kubernetes`
because the operator watches those APIs. It does not install their controllers or
exercise Kafka, PostgreSQL, 1Password, ingress routing, or telemetry delivery.

The cluster uses its own temporary kubeconfig and is deleted on success or failure.
Diagnostics are saved to `build/system-test/`. To inspect a failed cluster, run:

```bash
KEEP_CLUSTER=true scripts/test-system.sh
```

The script prints the cluster name and kubeconfig path. Use
`KUBECONFIG=<printed-path> kubectl ...` to inspect it, then
`kind delete cluster --name <printed-name>` when finished. You can override
`KIND_NODE_IMAGE`, `SYSTEM_TEST_TIMEOUT` (default `180s` per wait), and
`SYSTEM_TEST_ARTIFACTS` (diagnostics directory).

The **System Test** GitHub Actions workflow runs the same script for relevant pull
request changes and can also be triggered manually. The faster chart rendering
checks remain available through `scripts/test-helm-charts.sh`.
