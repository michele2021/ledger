#!/bin/bash

set -e
set -u

PGPASSWORD=${POSTGRES_PASSWORD}

create_app_role() {
  psql -v ON_ERROR_STOP=1 -U "${POSTGRES_USER}" -c "CREATE ROLE ${ACC_DB_USERNAME} WITH LOGIN PASSWORD '${ACC_DB_PASSWORD}'";
}

create_database() {
	database="$1"
	echo "Creating database: '${database}'"

	psql -v ON_ERROR_STOP=1 -U "${POSTGRES_USER}" <<-EOSQL
    SELECT 'CREATE DATABASE ${database}'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${database}')\gexec
EOSQL
}

if [ -n "${DATABASES_TO_CREATE}" ]; then

  echo "Creating 'app' role"
  create_app_role
  echo "Role created"

	echo "Databases creation requested: ${DATABASES_TO_CREATE}"
	for DB in $(echo "${DATABASES_TO_CREATE}" | tr ',' ' '); do
		create_database "${DB}";
		psql -v ON_ERROR_STOP=1 -U "${POSTGRES_USER}" --dbname "${DB}" <<-EOSQL
		  CREATE SCHEMA IF NOT EXISTS config;
		  CREATE SCHEMA IF NOT EXISTS infra;
		  GRANT CREATE ON DATABASE ${DB} TO ${ACC_DB_USERNAME};
      GRANT CREATE ON SCHEMA config TO ${ACC_DB_USERNAME};
      GRANT USAGE ON SCHEMA config TO ${ACC_DB_USERNAME};
      GRANT CREATE ON SCHEMA infra TO ${ACC_DB_USERNAME};
      GRANT USAGE ON SCHEMA infra TO ${ACC_DB_USERNAME};
      GRANT SELECT, INSERT, UPDATE, DELETE ON ALL tables IN SCHEMA config TO ${ACC_DB_USERNAME};
      GRANT SELECT, INSERT, UPDATE, DELETE ON ALL tables IN SCHEMA public TO ${ACC_DB_USERNAME};
      GRANT SELECT, INSERT, UPDATE, DELETE ON ALL tables IN SCHEMA infra TO ${ACC_DB_USERNAME};
      GRANT SELECT, UPDATE ON ALL sequences IN SCHEMA config TO ${ACC_DB_USERNAME};

      GRANT CREATE ON SCHEMA infra TO ${ACC_DB_USERNAME};
      GRANT USAGE ON SCHEMA infra TO ${ACC_DB_USERNAME};
      GRANT SELECT, INSERT, UPDATE, DELETE ON ALL tables IN SCHEMA infra TO ${ACC_DB_USERNAME};
      GRANT SELECT, UPDATE ON ALL sequences IN SCHEMA infra TO ${ACC_DB_USERNAME};

      GRANT CREATE ON SCHEMA datafix TO ${ACC_DB_USERNAME};
      GRANT USAGE ON SCHEMA datafix TO ${ACC_DB_USERNAME};
      GRANT SELECT, INSERT, UPDATE, DELETE ON ALL tables IN SCHEMA datafix TO ${ACC_DB_USERNAME};
      GRANT SELECT, UPDATE ON ALL sequences IN SCHEMA datafix TO ${ACC_DB_USERNAME};
EOSQL
	done
	echo "Databases created"
fi
