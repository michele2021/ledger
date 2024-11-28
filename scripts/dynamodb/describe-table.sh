#!/bin/bash

IMAGE_NAME="^localstack$"
TABLE_NAME="ledger-messages-development"
echo "awslocal dynamodb describe-table --region eu-central-1 --table-name ${TABLE_NAME}" |
docker exec -i "$(docker ps -f name=${IMAGE_NAME} -q)" /bin/bash

