![Build Container](https://github.com/rht-labs/lodestar-backend/workflows/Build%20Container/badge.svg)

# Lodestar - Backend

The API for Lodestar.

----

# JSON REST API

## OpenAPI Documentation of APIs

The JSON APIs are documented using using OpenAPI.  The OpenAPI UI can be used to view the exposed endpoints and interact directly with them.

Once the application is launched, the OpenAPI UI can be found at the following path:

```
http(s)://your-hostname[:port]/q/swagger-ui
```

## Available Resources

### Config

The `config` resource exposes endpoints that allow clients to retrieve application configuration data.

### Engagements

The `engagements` resource exposes various CRUD endpoints that allow clients to create, retrieve, update, and delete engagements.

#### GET Engagements API Parameters

`GET /engagements`

The following parameters are supported:

* Header Params

  * `Accept-version` - used to specify default results per page

* Query Params

  * `include` - attributes to include in response
  * `exclude` - attributes to exclude in response
  * `page` - page number to retrieve
if provided, the specified page will be returned. defaults to 1
  * `perPage` - number of records to retrieve for each page
    * if provided, the specified number of records will be returned
    * if header Accept-version is missing or set to v1, defaults to 500
    * if header Accept-version is specified and not v1, defaults to 20
  * `search` - query string to filter engagements
    * supports `=`, `like`, `not like`, `exists`, and `not exists`
    * `start` and/or `end` can be used to limit the results based on a date range (i.e. `start=2021-01-01&end=2021-05-01`)
  * `sortOrder` - ASC for ascending and DESC for descending. defaults to descending
  * `sortFields` - fields to sort on. defaults to customer_name,project_name

#### GET Engagement Nested Resource API Parameters

```
GET /engagements/customers/suggest
GET /engagements/artifacts
GET /engagements/artifacts/types
GET /engagements/categories
GET /engagements/hosting/environments
GET /engagements/scores
GET /engagements/usecases
```

The following parameters are supported:

* Query Params
  * `page` - page number to retrieve
    * if provided, the specified page will be returned. defaults to 1
  * `perPage` - number of records to retrieve for each page
      * if provided, the specified number of records will be returned
      * if header Accept-version is missing or set to v1, defaults to 500
      * if header Accept-version is specified and not v1, defaults to 20
  * `suggestion` - case insensitive query string to filter engagements
  * `sortOrder` - ASC for ascending and DESC for descending. defaults to descending


#### GET Engagement Dashboard/Query Helper API Parameters

```
GET /engagements/state/{state}
```

The following parameters are supported:

* Path Params

    * `state` - the state to filter engagements on
values are `upcoming`, `active`, `past`, and `terminating`
      * if unknown value supplied, `upcoming` will be used
* Header Params
  * supports all the same as GET /engagements
* Query Params
  * supports all the same as GET /engagements

```
GET /engagements/users/summary
```

The following parameters are supported:

* Query Params:
    * supports `=`, `like`, `not like`, `exists`, and `not exists`
    * `start` and/or `end` can be used to limit the results based on a date range (i.e. `start=2021-01-01&end=2021-05-01`)


### Status

The `status` resource exposes endpoints providing two main functionalities:

1. Application component status data
2. Webhook APIs to allow for updates to the database triggered from external changes.

### Version

The `version` resource exposes endpoints to retrieve application component versions.


----

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

----

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

