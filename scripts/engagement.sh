#!/bin/bash

command=$1
omphost=$2

if [ -z "$command" -o -z "$omphost" ]
then
  echo "usage:"
  echo "enagement.sh (refresh|create|update|get|process|launch|toggle) omp-backend-host[:port]"
  exit
fi

# get token to pass to API calls
token=`./get-token.sh`

if [ "$token" == "Environment Variables: TOKEN_USER, TOKEN_PASSWORD, and TOKEN_URL must be set.  These can be configured in token_config." ]
then
  echo "Failed to get token.  Please validate your token configuration"
  exit
fi

if [ "localhost:8080" == "$omphost" -o "localhost:8081" == "$omphost" ]
then
  host=http://${omphost}
else
  host=https://${omphost}
fi

if [ "refresh" == "$command" ]
then
  echo "running refresh command"
  curl -i -X PUT ${host}/engagements/refresh -H "Content-Type: application/json" -H "Authorization: Bearer ${token}"
fi

if [ "create" == "$command" ]
then
  echo "creating engagement"
  curl -i -X POST ${host}/engagements -d @data/create-engagement.json -H "Content-Type: application/json" -H "Authorization: Bearer ${token}"
fi

if [ "update" == "$command" ]
then
  echo "updating engagement"
  customer=$( jq -r .customer_name data/update-engagement.json)
  project=$( jq -r .project_name data/update-engagement.json)
  curl -i -X PUT ${host}/engagements/customers/${customer}/projects/${project} -d @data/update-engagement.json -H "Content-Type: application/json" -H "Authorization: Bearer ${token}"
fi

if [ "get" == "$command" ]
then
  echo "getting all engagements"
  curl -i -X GET ${host}/engagements -H "Content-Type: application/json" -H "Authorization: Bearer ${token}"
fi

if [ "process" == "$command" ]
then
  echo "pushing changes to git"
  curl -i -X PUT ${host}/engagements/process/modified -H "Content-Type: application/json" -H "Authorization: Bearer ${token}"
fi

if [ "launch" == "$command" ]
then
  echo "launching engagement"
  curl -i -X PUT ${host}/engagements/launch -d @data/launch-engagement.json -H "Content-Type: application/json" -H "Authorization: Bearer ${token}"
fi

if [ "toggle" == "$command" ]
then
  echo "launching engagement"
  curl -i -X PUT ${host}/engagements/autosave/toggle -H "Content-Type: application/json" -H "Authorization: Bearer ${token}"
fi