#!/bin/bash

command=$1
omphost=$2
customer=$3
project=$4

if [ -z "$command"  -o -z "$host" ]
then
  echo "usage:"
  echo "enagement.sh [refresh|create|update|get|process] omp-backend-host"
fi

json=$(curl -d "client_id=open-management-portal" -d "username=jacob" -d "password=password" -d "grant_type=password" https://sso-omp-jasee.apps.s11.core.rht-labs.com/auth/realms/omp/protocol/openid-connect/token)
token=$( jq -r ".access_token" <<<"$json" )

if [ "localhost:8080" == "$omphost" ]
then
  host=http://${omphost}
else
  host=https://${omphost}
fi

echo "using host - ${host}"

if [ "refresh" == "$command" ]
then
  echo "running refresh command"
  curl -i -X PUT ${host}/engagements/refresh -H "Content-Type: application/json" -H "Authorization: Bearer ${token}"
fi

if [ "create" == "$command" ]
then
  echo "creating engagement"
  curl -i -X POST ${host}/engagements -d @create-engagement.json -H "Content-Type: application/json" -H "Authorization: Bearer ${token}"
fi

if [ "update" == "$command" ]
then
  echo "updating engagement"
  curl -i -X PUT ${host}/engagements/customers/${customer}/projects/${project} -d @update-engagement.json -H "Content-Type: application/json" -H "Authorization: Bearer ${token}"
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
  curl -i -X PUT ${host}/engagements/launch -d @launch-engagement.json -H "Content-Type: application/json" -H "Authorization: Bearer ${token}"
fi