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
            // Keep integration schema/data migrations, then add test accounts when absent.
            DatabaseChangesExecutor.doMigration(handle, log, "test", "integration");
            seedAccountsIfMissing(handle);
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
        prepareSingleSchema(handle, "auth");
        prepareSingleSchema(handle, "object");
    }

    private void prepareSingleSchema(Handle handle, String schemaName) {
        handle.createUpdate("DROP SCHEMA IF EXISTS %s CASCADE".formatted(schemaName)).execute();
        handle.createUpdate("CREATE SCHEMA %s".formatted(schemaName)).execute();
    }

    private void seedAccountsIfMissing(Handle handle) {
        Integer accountCount = handle.select("SELECT COUNT(*) FROM config.account")
                .mapTo(Integer.class)
                .one();
        if (accountCount != null && accountCount > 0) {
            return;
        }

        insertAccount(handle, "CLIENTZERO", "Client");
        insertAccount(handle, "CLIENTONE", "Client");
        insertAccount(handle, "COMPANYZERO", "Company");
        insertAccount(handle, "COMPANYONE", "Company");
        insertAccount(handle, "ISSUERZERO", "Issuer");
    }

    private void insertAccount(Handle handle, String accountCode, String accountTypeCode) {
        String sql = """
                INSERT INTO config.account (accountCode, parentId, accountTypeId, status, description, test)
                SELECT :accountCode, NULL, at.accountTypeId, 'ACTIVE', :description, TRUE
                  FROM config.accountType at
                 WHERE at.accountTypeCode = :accountTypeCode
                   AND NOT EXISTS (
                        SELECT 1
                          FROM config.account a
                         WHERE a.accountCode = :accountCode
                           AND a.accountTypeId = at.accountTypeId
                   )
                """;

        handle.createUpdate(sql)
                .bind("accountCode", accountCode)
                .bind("accountTypeCode", accountTypeCode)
                .bind("description", "Auto-seeded account for DAO tests")
                .execute();
    }

}
