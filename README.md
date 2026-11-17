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

Run `./gradlew systemTest` from a checkout with Java 21, Docker, and Helm installed.
Docker must be running. The first run downloads
Gradle dependencies and container images; allow a few minutes and enough Docker
memory for a Kubernetes node and the operator (4 GiB is a useful starting point).

The Kotlin/JUnit suite lives in `src/test/system/kotlin`, with workload fixtures in
`src/test/system/resources`. Gradle builds the application JARs first. JUnit's
`@BeforeAll` builds the repository's Dockerfile, starts a disposable Testcontainers
K3s cluster, imports the locally built images, and installs both the CRD chart and
operator chart using Helm. It then creates a minimal `Application` and one-shot `Job`, checks the
application's readiness and HTTP response through its Service, checks successful
job completion and output, and verifies that deleting the custom resources removes
their generated workloads. Image builds use Testcontainers' `ImageFromDockerfile`
and image export uses its docker-java client. Fabric8 handles Kubernetes resources,
readiness, pod execution, and diagnostics. Helm is the only external CLI used by
the system tests.

The test installs the external CRD definitions from `src/main/resources/kubernetes`
because the operator watches those APIs. It does not install their controllers or
exercise Kafka, PostgreSQL, 1Password, ingress routing, or telemetry delivery.

Integration and system tests share `TcK3s.kt` from `src/test/common/kotlin`.
System tests request a uniquely named scoped container with reuse disabled, and
JUnit's `@AfterAll` stops only that container, including on test failure. The suite
uses its own temporary kubeconfig. Each workload test uses a separate namespace
and can run independently. This suite is opt-in and is not part of the default
`test` or `check` tasks. Every invocation runs against a fresh cluster.

Like the integration tests, use `TEST_KUBERNETES_VERSION` (for example `v1.35.1`)
and `TEST_KUBERNETES_IMAGE` (default `rancher/k3s`) to select the K3s image. The
shared helper appends `-k3s1` to the version; when unset it uses `latest`.

Diagnostics, including K3s container logs, are saved to `build/system-test/`, JUnit
XML to `build/test-results/systemTest/`, and the HTML report to
`build/reports/tests/systemTest/index.html`. Use Gradle's `--info` option to show
successful test output.

The **App Test** GitHub Actions workflow runs the same Gradle suite for relevant pull
request changes and can also be triggered manually. The faster chart rendering
checks remain available through `scripts/test-helm-charts.sh`.
