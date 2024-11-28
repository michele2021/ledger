#!/bin/bash

DIR=""
IMAGE_NAME="^localstack$"
BUCKET_NAME="clevercards-development-ledger-data"

if [ "$1" != "" ]; then
DIR="$1/"
fi

echo "awslocal s3 ls --region eu-central-1 s3://${BUCKET_NAME}/${DIR}" | docker exec -i "$(docker ps -f name=${IMAGE_NAME} -q)" /bin/bash
