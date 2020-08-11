#!/bin/bash

# 

if [ -z "$TOKEN_USER"  -o -z "$TOKEN_PASSWORD" -o -z "$TOKEN_URL" ] 
then

  # try to set the environment variables using token_config
  source token_config

  if [ "$TOKEN_USER" == "set" -o "$TOKEN_PASSWORD" == "set"  -o "$TOKEN_URL" == "set" ]
  then

    echo "Environment Variables: TOKEN_USER, TOKEN_PASSWORD, and TOKEN_URL must be set.  These can be configured in token_config."
    exit

  fi

fi

json=$(curl -d "client_id=lodestar" -d "username=$TOKEN_USER" -d "password=$TOKEN_PASSWORD" -d "grant_type=password" $TOKEN_URL)
token=$( jq -r ".access_token" <<<"$json" )

# return token
echo $token
