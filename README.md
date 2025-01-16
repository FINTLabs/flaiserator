# FLAISerator
![Build And Publish](https://github.com/FINTLabs/flaiserator/actions/workflows/build-publish.yaml/badge.svg) ![CodeQL](https://github.com/FINTLabs/flaiserator/actions/workflows/codeql.yaml/badge.svg)

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
