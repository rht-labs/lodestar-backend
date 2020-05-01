# Backend Test Scripts

## Overview

The scripts in this directory can be use for token generation and testing the Backend API.

## Configuration

The `token_config` file should be updated to match your token generation URL and with your credentials.  

```
# Replace with your configuration
export TOKEN_USER=set
export TOKEN_PASSWORD=set
export TOKEN_URL=set
```

Or you can set these environment variables manually however you like.

## Get Token Script

The `get-token.sh` script will source the `token_config` if the environment variables are not set.  It will use the provided credentials to call the token URL and print the `access_token` to the console.

This token can be used in the Swagger UI or as part of a Curl call to authenticate against the Backend APIs.

## Engagements Script

The `engagements.sh` script can be used to interact with the Backend API for engagement resources. The script will use the `get_token.sh` script to generate a token.  Then use that token to call the specified endpoint.

```
# usage
./enagements.sh action omp-backend-hostname
```

The currently defined actions are:

* `get` - returns all engagements from the requested omp-backend database
* `create` - creates an engagement using `data/create-engagement.json`
* `update` - updates an engagement using `data/update-engagement.json`
* `launch` - launches an engagement using `data/launch-engagement.json`
* `refresh` - purges the backend database and refreshes with the data from git
* `process` - pushes any udpated engagements in the backend database to git

The host name can be a local or remote instance, for example:

* `localhost:8080`
* `omp-backend.at.your.domain.com`

NOTE:  If using localhost:8080 or localhost:8081, the script assumes http will be used.  Otherwise, https will be used.