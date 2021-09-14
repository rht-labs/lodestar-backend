# Development on OpenShift

## Getting Started With Helm

This directory contains a Helm chart which can be used to deploy a development version of this app for rapid testing.

Before you use it, you will need to download & install Helm 3.

If you are not familiar with Helm - how to configure it and run - you can start with this quickstart:

[https://helm.sh/docs/intro/quickstart](https://helm.sh/docs/intro/quickstart)

## Using This Chart

1. Clone the target repo:

```
git clone https://github.com/rht-labs/lodestar-backend
```

2. Change into to the `deployment` directory:

```
cd lodestar-backend/deployment
```

3. Deploy using the following Helm command:

```shell script
helm template . \
  --values values-dev.yaml \
  --set git.uri=https://github.com/rht-labs/lodestar-backend.git \
  --set git.ref=master \
  --set jwtPublicKeyLocation=<your-jwt-public-key-location> \
  --set jwtIssuer=https:<your-jwt-issuer> \
  --set jwtEnable=true \
| oc apply -f -
```

It accepts the following variables

| Variable  | Use  |
|---|---|
| `git.uri`  | The HTTPS reference to the repo (your fork!) to build  |
| `git.ref`  | The branch name to build  |
| `jwtVerifyPublicKeyLocation`  | The URL at which your OpenID Connect (SSO) provider exposes its public key  |
| `jwtIssuer`  | The issuer specified JWT token|
| `jwtEnable`  | Flag to turn on and off JWT validation  |
| `token.webhook` | Accepts webhook token |

This will spin up all the usual resources that this service needs in production, plus a `BuildConfig` configured to build it from source from the Git repository specified. To trigger this build, use `oc start-build lodestar-backend`.

# Local Development

This service connects to a number of other services acting as a gate keeper via Auth and a coordinator to other services. Ensure you have access to other services

### Run locally

```bash
#some options
export LODESTAR_ENGAGEMENTS_API_URL=http://localhost:8080
export LODESTAR_BACKEND_GIT_COMMIT=gitSHA
export LODESTAR_BACKED_GIT_TAG=v34.4
export JWT_LOGGING=INFO
export LODESTAR_BACKEND_VERSIONS_PATH=$PWD/version-manifest.yml

mvn quarkus:dev
```

Navigate to http://localhost:8081

### Testing

Running mvn quarkus:test will allow for continuous testing
