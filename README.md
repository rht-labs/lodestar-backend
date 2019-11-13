# Open Management Portal - Backend

> The API for the Open Management Portal.

## Configuration

The following environment variables are available:

| Name | Example Value | Required |
|------|---------------|----------|
| MP_JWT_VERIFY_PUBLICKEY_LOCATION | http://[your-cluster-internal-sso-service-name]:8080/auth/realms/[your-realm-id]/protocol/openid-connect/certs | True |

## Deployment

This project includes an `openshift-applier` inventory. To use it, make sure that you are logged in to the cluster and that you customize the variables in `.applier/inventory/group_vars/all.yml`. Once these are configured, you can deploy the project with:

```bash
$ cd .applier/

$ ansible-galaxy install -r requirements.yml --roles-path=roles --force

$ ansible-playbook apply.yml -i inventory/
```

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
