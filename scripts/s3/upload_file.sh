#!/bin/bash

IMAGE_NAME="^localstack$"
BUCKET_NAME="clevercards-development-ledger-data"
CONTAINER_ID="$(docker ps -f name=${IMAGE_NAME} -q)"
REPORT_NAME="EPAYIT_D-SALES-REPORT_$(date +%F%T | sed 's/[-|:]//g').csv"
docker cp ./EPAYIT_D-SALES-REPORT_TEST.csv "${CONTAINER_ID}:/${REPORT_NAME}"
echo "awslocal s3 cp /${REPORT_NAME} s3://${BUCKET_NAME}/in/epay_it/dsr/ --region eu-central-1" | docker exec -i "${CONTAINER_ID}" /bin/bash
