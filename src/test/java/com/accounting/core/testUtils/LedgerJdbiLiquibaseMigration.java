package com.accounting.core.testUtils;

import com.accounting.commons.db.DatabaseChangesExecutor;
import liquibase.exception.LiquibaseException;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.testing.junit5.JdbiExtensionInitializer;

import javax.sql.DataSource;

/**
 * LedgerJdbiLiquibaseMigration - a bridge to run Liquibase migrations to initialize DB for unit tests.
 */
@Slf4j
public class LedgerJdbiLiquibaseMigration implements JdbiExtensionInitializer {

    final boolean recreateSchema;

    public LedgerJdbiLiquibaseMigration(boolean recreateSchema) {
        this.recreateSchema = recreateSchema;
    }

    @Override
    public void initialize(DataSource ds, Handle handle) {
        try {
            if (recreateSchema) {
                prepareSchema(handle);
            }
            //Note: use the "integration", as it initializes some example data.
            DatabaseChangesExecutor.doMigration(handle, log, "test", "integration");
        } catch (LiquibaseException e) {
            String msg = "LiquibaseException: Unable to execute some migration scripts";
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    private void prepareSchema(Handle handle) {
        prepareSingleSchema(handle, "public");
        prepareSingleSchema(handle, "config");
        prepareSingleSchema(handle, "infra");
    }

    private void prepareSingleSchema(Handle handle, String schemaName) {
        handle.createUpdate(String.format("DROP SCHEMA IF EXISTS %s CASCADE", schemaName)).execute();
        handle.createUpdate(String.format("CREATE SCHEMA %s", schemaName)).execute();
    }

}
