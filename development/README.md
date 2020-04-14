# Development on OpenShift

## Getting Started With Helm

This directory contains a Helm chart which can be used to deploy a development version of this app for rapid testing.

Before you use it, you will need to download & install Helm 3.

If you are not familiar with Helm - how to configure it and run - you can start with this quickstart:

[https://helm.sh/docs/intro/quickstart](https://helm.sh/docs/intro/quickstart)

## Using This Chart

1. Clone the target repo:

```
git clone https://github.com/rht-labs/open-management-portal-backend
```

2. Change into to the `development` directory:

```
cd open-management-portal-backend/development
```

3. Deploy using the following Helm command:

```shell script
helm template . \
  --set git.uri=https://github.com/dwasinge/open-management-portal-backend.git \
  --set git.ref=master \
  --set ompGitlabApiUrl=https://omp-git-api-omp-dev.apps.s11.core.rht-labs.com \
  --set jwtPublicKeyLocation=https://sso-omp-jasee.apps.s11.core.rht-labs.com/auth/realms/omp/protocol/openid-connect/certs \
  --set jwtIssuer=https://sso-omp-jasee.apps.s11.core.rht-labs.com/auth/realms/omp \
  --set jwtEnable=true \
  --set mongodbServiceName=omp-backend-mongodb \
  --set mongodbUser=mongouser \
  --set mongodbPassword=mongopassword \
  --set mongodbDatabase=engagements \
  --set mongodbAdminPassword=mongoadminpassword \
| oc apply -f -
```

mongodbServiceName: omp-backend-mongodb
mongodbUser: false
mongodbPassword: false
mongodbDatabase: false
mongodbAdminPassword: false


It accepts the following variables

| Variable  | Use  |
|---|---|
| `git.uri`  | The HTTPS reference to the repo (your fork!) to build  |
| `git.ref`  | The branch name to build  |
| `cacheService`  | The service name of the Infinispan deployment  |
| `ompGitlabApiUrl`  | URL for the route or service to the Git API service  |
| `jwtVerifyPublicKeyLocation`  | The URL at which your OpenID Connect (SSO) provider exposes its public key  |
| `jwtIssuer`  | The issuer specified JWT token|
| `jwtEnable`  | Flag to turn on and off JWT validation  |

This will spin up all of the usual resources that this service needs in production, plus a `BuildConfig` configured to build it from source from the Git repository specified. To trigger this build, use `oc start-build omp-backend`.
