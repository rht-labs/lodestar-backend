# Development on OpenShift

## Getting Started With Helm

This directory contains a Helm chart which can be used to deploy a development version of this app for rapid testing.

Before you use it, you will need to download & install Helm 3.

If you are not familiar with Helm - how to configure it and run - you can start with this quickstart:

[https://helm.sh/docs/intro/quickstart](https://helm.sh/docs/intro/quickstart)

## Using This Chart

Move to the `development` directory (i.e. `cd development/`), and deploy using the following Helm command:

```shell script
helm template . \
  --set git.uri=https://github.com/rht-labs/open-management-portal-backend.git \
  --set git.ref=master \
  --set cacheService=<your-infinispan-service-name> \
  --set cacheUseAuth=<use-infinispan-auth> \
  --set configRepositoryId=<your-config-repository-id> \
  --set deployKey=<your-gitlab-deploy-key-id> \
  --set gitLabApiUrl=<your-gitlab-base-url> \
  --set gitLabPersonalAccessToken=<your-gitlab-personal-access-token> \
  --set jwtVerifyPublicKeyLocation=<your-openid-connect-cert-url> \
  --set residenciesParentRepositoryId=<your-gitlab-group-id> \
  --set templateRepositoryId=<your-template-repository-id> \
  --set trustedClientKey=<your-trusted-client-key> \
| oc apply -f -
```

It accepts the following variables

| Variable  | Use  |
|---|---|
| `git.uri`  | The HTTPS reference to the repo (your fork!) to build  |
| `git.ref`  | The branch name to build  |
| `cacheService`  | The service name of the Infinispan deployment  |
| `cacheUseAuth`  | Enable authentication against the cache  |
| `configRepositoryId`  | The GitLab ID of the config repository  |
| `deployKey`  | The ID of the GitLab deploy key to enable on newly-created repositories  |
| `gitLabApiUrl`  | The base URL of the GitLab instance to use  |
| `gitLabPersonalAccessToken`  | The access token to use to auth against GitLab  |
| `jwtVerifyPublicKeyLocation`  | The URL at which your OpenID Connect (SSO) provider exposes its public key  |
| `residenciesParentRepositoryId`  | The ID of the GitLab group under which to create new projects  |
| `templateRepositoryId`  | The ID of the GitLab repository which defines a template to use for creating new repos  |
| `trustedClientKey`  | [Temporary] Used as a placeholder to authenticate client requests, being replaced by validating JWT tokens against the `jwtVerifyPublicKeyLocation` |

This will spin up all of the usual resources that this service needs in production, plus a `BuildConfig` configured to build it from source from the Git repository specified. To trigger this build, use `oc start-build omp-backend`.
