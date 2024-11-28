#!/bin/bash

IMAGE_NAME="^localstack$"
echo "awslocal dynamodb list-tables --region eu-central-1" | docker exec -i "$(docker ps -f name=${IMAGE_NAME} -q)" /bin/bash
