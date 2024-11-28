#!/bin/bash

QUEUE_NAME="accounting-development.fifo"
IMAGE_NAME="^localstack$"
echo "awslocal sqs receive-message --region eu-central-1  --visibility-timeout 1 --wait-time-seconds 5 \
--queue-url http://localstack:4566/000000000000/${QUEUE_NAME} --max-number-of-messages 10" |
docker exec -i "$(docker ps -f name=${IMAGE_NAME} -q)" /bin/bash
