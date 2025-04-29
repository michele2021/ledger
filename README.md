# ledger

### Requirements:
+ Java 21
+ Gradle 8.12.1
+ Postgres 17.2
+ Docker and Docker-Compose
+ Terraform 1.1.8



### Application Env variables:
* Running environment:
  > * `ENVIRONMENT` &nbsp;&nbsp;&nbsp;(`integration`, `staging`, etc.)

* API authentication:
  > * `API_KEY`
* Server:
  > * `ACCOUNTING_HTTP_APP` &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;default - `"8080"`
  > * `ACCOUNTING_HTTP_ADMIN` &nbsp;&nbsp;&nbsp;default - `"8081"`

* Postgres database
  > * `PG_HOST`
  > * `PG_PORT`
  > * `ACC_DB_USERNAME`
  > * `ACC_DB_PASSWORD`
  > * `ACC_OWNER_DB_PASSWORD`

* Amazon AWS:
  > * `AWS_REGION`
  > * `AWS_DYNAMODB_TTL_DAYS` (default - `180 days`)
  > * `AWS_DYNAMO_DB_MESSAGES_TABLE_NAME` (default - `ledger-messages-$ENVIRONMENT`)
  > * `AWS_SQS_ACCOUNTING_QUEUE` (default - `accounting-$ENVIRONMENT.fifo`)
  > * `AWS_SQS_ACCOUNTING_BALANCE_QUEUE` (default - `accountingbalance-$ENVIRONMENT.fifo`
  > * `AWS_SQS_WAIT_TIME_SEC` (default - `20`)
  > * `AWS_SQS_MESSAGES_BATCH_SIZE` (default - `10`)
  > * `AWS_SNS_SYSTEM_NOTIFICATIONS_ARN` (default - `arn:aws:sns:$AWS_REGION:000000000000:ledger-system-notifications-$ENVIRONMENT`)
  > * `AWS_SNS_FINANCIAL_NOTIFICATIONS_ARN` (default - `arn:aws:sns:$AWS_REGION:000000000000:ledger-financial-notifications-$ENVIRONMENT`)
  > * `AWS_ENDPOINT` (optional - `overrides the default AWS communication endpoint`)

* Authentication service:
  > * `AUTH_JWKS_ENCODE`

* CORS config:
  > * `ALLOWED_ORIGINS_APP` &nbsp;&nbsp;default - `"*"`
  > * `ALLOWED_METHODS_APP` &nbsp;&nbsp;default - `OPTIONS,GET,PUT,POST,DELETE,HEAD`
  > * `ALLOWED_HEADERS_APP` &nbsp;&nbsp;default - `X-Requested-With,Content-Type,Accept,Origin,X-API-KEY`
  > * `ALLOWED_ORIGINS_ADMIN` &nbsp;&nbsp;default - `"*"`
  > * `ALLOWED_METHODS_ADMIN` &nbsp;&nbsp;default - `OPTIONS,GET,PUT,POST,DELETE,HEAD`
  > * `ALLOWED_HEADERS_ADMIN` &nbsp;&nbsp;default - `X-Requested-With,Content-Type,Accept,Origin,X-API-KEY`

* Debug tools config:
  > * `LEDGER_AUTO_SKIP_MESSAGE` default - `false`
  > * `DSR_SNS_NOTIFICATIONS_ENABLED` default - `false`
  
### Used ports

Please make sure all following ports used by docker are available, or simply adjust environments in .env file to your needs.
<table>
  <tbody>
    <th>Service</th>
    <th>URL</th>
    <th>Docker URL</th>
    <th>Port</th>
    <tr>
      <td>AWS Localstack</td>
      <td>http://localhost</td>
      <td>http://localstack</td>
      <td style="color:darkorange; font-weight: bold">4566</td>
    </tr>
    <tr>
      <td>PostgreSQL DB</td>
      <td>http://localhost</td>
      <td>http://postgresql_db</td>
      <td style="color:darkorange; font-weight: bold">5432</td>
    </tr>
    <tr>
      <td>App API</td>
      <td>http://localhost</td>
      <td>http://accounting_core</td>
      <td style="color:darkorange; font-weight: bold">8080</td>
    <tr>
      <td>App Metrics</td>
      <td>http://localhost</td>
      <td>http://accounting_core</td>
      <td style="color:darkorange; font-weight: bold">8081</td>
    </tr>
    <tr>
      <td>App Debug</td>
      <td>http://localhost</td>
      <td>http://accounting_core</td>
      <td style="color:darkorange; font-weight: bold">8090</td>
    </tr>
  </tbody>
</table>

### Open API

+ http://localhost:8086/swagger.json  (display)
+ http://localhost:8086/swagger.yaml  (download)


### Gradle Tasks

This project defines custom Gradle tasks to assist with development and documentation.

#### Available Tasks

| Task Name         | Description                                         |
|-------------------|-----------------------------------------------------|
| `generateOpenAPI` | Generates `swagger.json` and `swagger.yaml` files for the application's APIs. |


### Run the app
First, initialize the gradle wrapper. This is required to execute run.sh.
```
gradle wrapper --gradle-version 7.1.1
```
To run application in your local environment all you need is Docker installed. <br/>
Automation script will handle *PostgreSQL DBs* creation as well as AWS Localstack resources, such as *SQS* queues *DynamoDB* etc.<br/>
It will also handle building the fat jar if it does not exist.<br/>

To run the project make sure your in the project root directory, and simply execute `./run.sh` cmd in the bash terminal.
```
./run.sh
```
If the jar already exists, and you'd like to rebuild the project `--build` param can be used to force the clean start up.
```
./run.sh --build
```
### Stopping application and Docker containers
Stopping and destroying resources have been also automated by providing `./shutdown.sh` script. <br/>
By default `./shutdown.sh` stops the application and all other resources such as DB and AWS localstack resources.<br/>
To stop application without stopping the DB and anything else `--stop` or `-s` flags can be added: <br/>
```
./shutdown.sh --stop   or   ./shutdown.sh -s
```

### Destroy resources:
By default, the ```shutdown``` script does not remove attached volumes, so the data can be persisted between the restarts.  
It is possible tho to remove the persistence layer by adding `--volume` or `-v` flag.
```
./shutdown.sh --volumes   or   ./shutdown.sh -v   or   ./shutdown.sh -V
```

The application can also run outside of Docker:
```
gradle run

gradle clean runShadow

java -jar build/libs/ledger*-all.jar server server.yaml
```
