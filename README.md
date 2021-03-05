![Build Container](https://github.com/rht-labs/lodestar-backend/workflows/Build%20Container/badge.svg)

# Lodestar - Backend

The API for Lodestar.

## JSON REST APIs

The JSON REST APIs consist of three resources types:

* config
* engagements
* status
* version

The application, once running, also exposes a Swagger UI that will provide more details about each of the APIs described below.  It can be found using the `/swagger-ui` path for the application.

### Config Resource

The config resource exposes an API that will return the configured config file from git using the [Git API](https://github.com/rht-labs/lodestar-git-api).

```
GET /config
```

It's recommended to add the version header with the a version above v1. v1 returns yaml wrapped json. Fun! v2 and forward return pure json with all git information stripped out.

### Engagement Resource

The engagements resource exposes an API that allows clients to create, retrieve, and delete engagement resources.  

The unique key for an engagement is its `uuid` attribute.  

The following endpoints are available for engagement resources:

```
POST ​/engagements
```
Creates an engagement in the database and triggers event to push to GitLab.
```
PUT ​/engagements​/{uuid}
```
Updates the engagement with the given UUID in the database and triggers event to push to GitLab.
```
DEPRECATED: PUT ​/engagements​/customers​/{customerName}​/projects​/{projectName}
```
Updates the engagement with the given UUID in the database and triggers event to push to GitLab.
```
PUT​/engagements​/launch
```
Sets the engagement launch data in the database and triggers event to push to GitLab.
```
PUT ​/engagements​/refresh
```
Starts the process to insert any engagements in GitLab, but not in the database, into the database.  If the query parameter `purgeFirst` is set to true, the database will be cleared before inserting.
```
PUT ​/engagements​/uuids​/set
```
Starts the process to set any UUIDs for engagement or engagement users if missing.  All new engagements and users will have a UUID assigned at time of creation.  This process was for engagements and users created before functionality existed.
```
GET /engagements
```
Returns all engagements currently found in the database.
```
GET ​/engagements​/{uuid}
```
Returns the engagement for the given UUID.  A 404 will be returned if not found.
```
GET ​/engagements​/customers​/{customerName}​/projects​/{projectName}
```
Returns the engagement for the given UUID.  A 404 will be returned if not found.
```
GET ​/engagements​/artifact​/types
```
Returns all artifact types found in the database or ones that match the optional query param `suggest`.
```
GET ​/engagements​/categories
```
Returns all categories in the database or ones that match the optional query param `suggest`.
```
GET ​/engagements​/customers​/suggest
```
Returns a list of all customer names that match the required query param `suggest`.
```
HEAD ​/engagements​/{uuid}
```
Returns metadata for the engagement that matches provided UUID.
```
DEPRECATED: HEAD ​/engagements​/customers​/{customerName}​/projects​/{projectName}
```
Returns metadata for the engagement that matches provided UUID.
```
HEAD ​/engagements​/subdomain​/{subdomain}
```
Returns a 409 status if subdomain is already used.  Otherwise, a 200 is returned.
```
DELETE ​/engagements​/{id}
```
Deletes the engagement with the given UUID in the database and triggers event to push to GitLab.

### Status Resource

The status resource exposes APIs that allow external applications to notify the backend of changes.  It also exposes a proxy to the LodeStar Status API to get the current component status of all LodeStar components.

```
GET ​/status
```
Returns status of all configured components.
```
POST /status​/deleted
```
Triggers the deletion of an engagement with the customer and engagement names found in the body of the message.
```
POST /status​/hook
```
Triggers the update of Status and Commit data for the engagement that matches the customer and engagement names found in the body of the message.

### Version Resource

The version resource exposes an endpoint that will allow the client to determine which versions of the backend, git api, and other components being used by Lodestar.

```
GET  /api/v1/version
```

## Scheduled Jobs

There are currently 2 scheduled jobs that run on a given active node of the LodeStar Backend.

### GitLab to Database sync

This job is responsible for inserting into the databse any engagement in GitLab that is not currently in the database.

The job interval can be set using the environment variable `AUTO_REPOP_CRON`, but is defaulted to every 5 minutes.

For example:
```
auto.repopulate.cron.expr=${AUTO_REPOP_CRON:0 0/5 * * * ?}
```

By default, this does not insert or update any engagement that is already in the database.  Repopulation of the entire database can be triggered using the `PUT ​/engagements​/refresh` API and setting the query param `purgeFirst=true`.

### Set Missing UUIDs

This job is used to check once at startup for any engagements missing UUIDs for either the engagement or the engagement users.

## Configuration

The following environment variables are available:

### Logging
| Name | Example Value | Required |
|------|---------------|----------|
| JWT_LOGGING| INFO | False |
| LODESTAR_BACKEND_LOGGING | INFO | False |
| LODESTAR_BACKEND_MIN_LOGGING | TRACE | false |

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
| DATABASE_SERVICE_NAME | lodestar-mongodb | True |
| MONGODB_DATABASE | engagements | True |


### Git API

| Name | Example Value | Required |
|------|---------------|----------|
| LODESTAR_GITLAB_API_URL   | http://lodestar-git-api:8080 | True |

### Status API

| Name | Example Value | Required |
|------|---------------|----------|
| LODESTAR_STATUS_API_URL |  http://lodestar-status:8080 | True |

### Version Resource

| Name | Example Value | Required |
|------|---------------|----------|
| LODESTAR_BACKEND_GIT_COMMIT | not.set | False |
| LODESTAR_BACKEND_GIT_TAG | not.set | False |

### Status Resource

| Name | Example Value | Required |
|------|---------------|----------|
| WEBHOOK_TOKEN | myToken | True |
| CLEANUP_TOKEN | cToken | False |

### Engagements Resource

| Name | Example Value | Required |
|------|---------------|----------|
| COMMIT_FILTERED_MESSAGE_LIST | manual_refresh | False |

### Git Database Sync

| Name | Example Value | Required |
|------|---------------|----------|
| AUTO_REPOP_CRON | 0 0/5 * * * ? | False |

### Git Database Sync

| Name | Example Value | Required |
|------|---------------|----------|
| EVENT_MAX_RETRIES | 5 | False |
| EVENT_RETRY_DELAY_FACTOR | 2 | False |
| EVENT_RETRY_MAX_DELAY | 60 | False |
| EVENT_GET_PER_PAGE | 20 | False |

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

