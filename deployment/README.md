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

2. Change into to the `deployment` directory:

```
cd open-management-portal-backend/deployment
```

3. Deploy using the following Helm command:

```shell script
helm template . \
  --values values-dev.yaml \
  --set git.uri=https://github.com/rht-labs/open-management-portal-backend.git \
  --set git.ref=master \
  --set ompGitlabApiUrl=http://omp-git-api:8080 \
  --set jwtPublicKeyLocation=<your-jwt-public-key-location> \
  --set jwtIssuer=https:<your-jwt-issuer> \
  --set jwtEnable=true \
  --set mongodbServiceName=omp-backend-mongodb \
  --set mongodbUser=<your-mongodb-user> \
  --set mongodbPassword=<your-mongodb-password> \
  --set mongodbDatabase=engagements \
  --set mongodbAdminPassword=<your-mongodb-admin-password> \
| oc apply -f -
```

It accepts the following variables

| Variable  | Use  |
|---|---|
| `git.uri`  | The HTTPS reference to the repo (your fork!) to build  |
| `git.ref`  | The branch name to build  |
| `ompGitlabApiUrl`  | URL for the route or service to the Git API service  |
| `jwtVerifyPublicKeyLocation`  | The URL at which your OpenID Connect (SSO) provider exposes its public key  |
| `jwtIssuer`  | The issuer specified JWT token|
| `jwtEnable`  | Flag to turn on and off JWT validation  |
| `mongodbServiceName` | MongoDB service name |
| `mongodbUser` | Application user for MongoDB |
| `mongodbPassword` | Application user password for MongoDB |
| `mongodbDatabase` | Application database name |
| `mongodbAdminPassword` | Admin password for MongoDB |

This will spin up all of the usual resources that this service needs in production, plus a `BuildConfig` configured to build it from source from the Git repository specified. To trigger this build, use `oc start-build omp-backend`.
