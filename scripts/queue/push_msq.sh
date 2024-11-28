#!/bin/bash

ERROR="\033[0;31mERROR: \033[0m"
QUEUE_NAME="accounting-development.fifo"

runInContainer() {
  docker exec -i "$(docker ps -f name="^localstack$" -q)" /bin/bash
}

if [ "$#" -eq "0" ]; then
echo -e "${ERROR}Params missing! Path to json file with messages"

elif [ "${1}" != "" ]; then
JSON=$(cat "${1}")
echo "awslocal sqs send-message-batch --region eu-central-1 \
--queue-url http://localstack:4566/000000000000/${QUEUE_NAME} --entries '${JSON}'" | runInContainer
else
echo -e "${ERROR}Invalid param"
fi





