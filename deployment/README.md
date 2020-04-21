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

# Local Development

### Mongo DB

If you are developing locally you will need to connect to a database. By default it connects to a local instead. You can use the docker setup located in the `development` directory in this repo to deploy an instance. It will persist data into a sub-directory named `mongo-volume`. To destroy the database entirely you can shut down the docker instance and remove the sub-directory.

From the root of the repo

```bash
cd deployment
docker-compose up
```

### Local Git API

Clone the Git API repo, follow the local dev steps there and run it on port 8080.

### Run locally

```bash
# required
export OMP_GITLAB_API_URL=http://localhost:8080

#some options
# could include not default mongo values here
export OMP_BACKEND_GIT_COMMIT=gitSHA
export OMP_BACKED_GIT_TAG=v34.4
export JWT_LOGGING=INFO
export OMP_BACKEND_VERSIONS_PATH=$PWD/version-manifest.yml

echo "OMP GIT API $OMP_GITLAB_API_URL"
mvn quarkus:dev
```

Navigate to http://localhost:8080