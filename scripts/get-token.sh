#!/bin/bash

username=$1
password=$2

if [ -z "$username"  -o -z "$password" ]
then
  echo "usage:"
  echo "get-token.sh username password"
fi

json=$(curl -d "client_id=open-management-portal" -d "username=${username}" -d "password=${password}" -d "grant_type=password" https://sso-omp-jasee.apps.s11.core.rht-labs.com/auth/realms/omp/protocol/openid-connect/token)
token=$( jq -r ".access_token" <<<"$json" )

echo $token
