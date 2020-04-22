![Build Container](https://github.com/rht-labs/open-management-portal-backend/workflows/Build%20Container/badge.svg)

# Open Management Portal - Backend

The API for the Open Management Portal.

## JSON REST APIs

The JSON REST APIs consist of three resources types:

* config
* engagements
* git sync
* version

The application, once running, also exposes a Swagger UI that will provide more details about each of the APIs described below.  It can be found using the `/swagger-ui` path for the application.

### Config Resource

The config resource exposes an API that will return the configured config file from git using the Git API.

```
GET /config
```

### Engagement Resource

The engagements resource exposes an API that allows clients to create, retrieve, and delete engagement resources.  The unique key for an engagement consists of `customer_name` and `project_name`.  The following endpoints will update the configured Mongo DB and mark the records as modified so an asynchronous process can push the changes to Gitlab using the Git API.

```
# create an engagement
POST /engagements
# update a specific engagement
PUT  /engagements/customers/{customerId}/projects/{projectId}
# adds launch data to an engagement and syncs with git api
PUT  /engagements/launch
# retrieve all engagements
GET  /engagements
# retrieve a specific engagement
GET  /engagements/customers/{customerId}/projects/{projectId}
```

### Git Sync Resource

There are two exposed endpoints that will allow clients to deliberately sync data from Mongo DB to Gitlab using the Git API or to clear the data from the Mongo DB and insert all engagements from Gitlab.

```
# push all modified resources from Mongo DB to Gitlab
PUT  /engagements/process/modified
# clear Mongo DB, replace with engagement data from Gitlab
PUT  /engagements/refresh
```

### Version Resource

The version resource exposes an endpoint that will allow the client to determine which versions of the backend, git api, and other components being used by OMP.

```
GET  /api/v1/version
```

## Scheduled Auto Sync to Git API

A configurable auto sync feature allows data that has been modified in Mongo DB to be pushed to Gitlab using the Git API.  This feature is configured using a CRON expression that can be updated in the application.properties file or overridden using environment variables.

```
# defaults to sync every 30 seconds
auto.save.cron.expr=0/30 * * * * ?
```

__NOTE:__ There is no current auto sync from Gitlab to Mongo DB.  The `/engagements/refresh` API can be used to force a refresh of the data in Mongo DB from Gitlab if changes have been made without using the Backend APIs

## Configuration

The following environment variables are available:

### Logging
| Name | Example Value | Required |
|------|---------------|----------|
| JWT_LOGGING| INFO | False |
| OMP_BACKEND_LOGGING | INFO | False |

### JWT

| Name | Example Value | Required |
|------|---------------|----------|
| JWT_PUBKICKEY_LOCATION | http://[your-cluster-internal-sso-service-name]:8080/auth/realms/[your-realm-id]/protocol/openid-connect/certs | True |
| JWT_ISSUER | http://[your-cluster-internal-sso-service-name] | True |
| JWT_ENABLE | True | False |

### Mongo DB

| Name | Example Value | Required |
|------|---------------|----------|
| MONGODB_USER | monguser | True |
| MONGODB_PASSWORD | mongopassword | True |
| DATABASE_SERVICE_NAME | omp-backend-mongodb | True |
| MONGODB_DATABASE | engagements | True |

### Config Resource

| Name | Example Value | Required |
|------|---------------|----------|
| CONFIG_REPOSITORY_ID |  1234             |  True        |
| CONFIG_FILE | schema/config.yml | False |

### Git API

| Name | Example Value | Required |
|------|---------------|----------|
| OMP_GITLAB_API_URL   | http://omp-git-api:8080 | True |

### Version Resource

| Name | Example Value | Required |
|------|---------------|----------|
| OMP_BACKEND_GIT_COMMIT | not.set | False |
| OMP_BACKEND_GIT_TAG | not.set | False |
| OMP_BACKEND_VERSIONS_PATH | /config/version-manifest.yml | False |

### Git Auto Sync

| Name | Example Value | Required |
|------|---------------|----------|
| AUTO_SAVE_CRON_EXPR | 0/30 * * * * ? | False |

## Development

See [the development README](deployment/README.md) for details on how to spin up a deployment for developing on OpenShift.

## Components

This project was built using Quarkus.

## Testing

This project runs tests using an embedded Mongo DB and Using the TokenUtils found in the test directory.  The only component Mocked is the external REST Client calls to the Git API.

## Useful Commands

``` bash
# serve with hot reload at localhost:8080
mvn quarkus:dev
# run unit tests
mvn test
# build for production
mvn quarkus:build
```

