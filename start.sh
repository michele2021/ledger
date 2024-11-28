#!/bin/sh

java -cp /accountingApp.jar com.accounting.commons.db.DatabaseChangesExecutor $ENVIRONMENT $PG_HOST "accounting" $ACC_DB_USERNAME $ACC_DB_PASSWORD && java -jar /accountingApp.jar server server.yaml
