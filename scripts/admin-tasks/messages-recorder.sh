#!/bin/bash

if [ "$1" == "start" ] || [ "$1" == "stop" ]; then
curl -i -H "ContentType: application/json" -X POST "http://localhost:8081/tasks/messages-recorder?action=${1}"
else
  echo "Invalid param - valid input 'start' or 'stop'"
  exit 1
fi


