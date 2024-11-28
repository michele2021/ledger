#!/bin/bash

echo "awslocal sqs purge-queue --region eu-central-1 --queue-url http://localstack:4566/000000000000/accounting-development.fifo" |
docker exec -i "$(docker ps -f name="^localstack$" -q)" /bin/bash
