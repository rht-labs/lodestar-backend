# Open Management Portal - Backend

> The API for the Open Management Portal.

## Configuration

The following environment variables are available:

| Name | Example Value | Required |
|------|---------------|----------|
| MP_JWT_VERIFY_PUBLICKEY_LOCATION | http://[your-cluster-internal-sso-service-name]:8080/auth/realms/[your-realm-id]/protocol/openid-connect/certs | True |

## Components

This project was built using Quarkus.

## Testing

This project runs unit tests using an alternate application profile, which disables JWT token verification and user role verification. This means that all endpoints are available without returning 401 or 403, but the user context is not available during tests.

## Useful Commands

``` bash

# serve with hot reload at localhost:8080
$ mvn quarkus:dev

# run unit tests
$ mvn test

# build for production
$ mvn quarkus:build

```
