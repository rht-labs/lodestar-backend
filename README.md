![Build Container](https://github.com/rht-labs/open-management-portal-backend/workflows/Build%20Container/badge.svg)

# Open Management Portal - Backend

> The API for the Open Management Portal.

## Configuration

The following environment variables are available:

| Name | Example Value | Required |
|------|---------------|----------|
| JWT_PUBKICKEY_LOCATION | http://[your-cluster-internal-sso-service-name]:8080/auth/realms/[your-realm-id]/protocol/openid-connect/certs | True |
| JWT_ISSUER | http://[your-cluster-internal-sso-service-name] | True |
| JWT_ENABLE | True | False |
| MONGODB_USER | monguser | True |
| MONGODB_PASSWORD | mongopassword | True |
| DATABASE_SERVICE_NAME | omp-backend-mongodb | True |
| MONGODB_DATABASE | engagements | True |
| CONFIG_REPOSITORY_ID |  1234             |  True        |
| CONFIG_REPOSITORY_PATH | schema/config.yml | True |
| OMP_GITLAB_API_URL   | http://omp-git-api:8080 | True |

## Development

See [the development README](deployment/README.md) for details on how to spin up a deployment for developing on OpenShift.

## Components

This project was built using Quarkus.

## Testing

This project runs unit tests using an alternate application profile, which disables JWT token verification and user role verification. This means that all endpoints are available without returning 401 or 403, but the user context is not available during tests.

## Useful Commands

``` bash
# serve with hot reload at localhost:8080
mvn quarkus:dev
# run unit tests
mvn test
# build for production
mvn quarkus:build
```
