#!/bin/bash

IMAGE_NAME="^localstack$"
BUCKET_NAME="clevercards-development-ledger-data"
echo "awslocal s3 rm --recursive --region eu-central-1 s3://${BUCKET_NAME}" | docker exec -i "$(docker ps -f name=${IMAGE_NAME} -q)" /bin/bash
