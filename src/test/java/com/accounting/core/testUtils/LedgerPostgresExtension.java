package com.accounting.core.testUtils;

import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.ConnectionException;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.JdbiExternalPostgresExtension;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.postgresql.ds.PGSimpleDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.accounting.core.testUtils.LedgerPostgresExtension.Mode.FOR_EACH_CLASS;
import static com.accounting.core.testUtils.LedgerPostgresExtension.Mode.FOR_EACH_METHOD;


/**
 * Junit5 extension to work with Postgres through Jdbi.
 * Features:
 *   * Works with CloudBees CodeShip platform, use included Postgres server.
 *   * For local development can automatically create a Docker container that imitate one provided by CodeShip platform.
 *   * Allow DB initialization to be executed just once for a test class, saving on initialization time, but require that test does not dirty database.
 *   * Provides automatic transaction and rollback on shared handle in the FOR_EACH_CLASS mode, trying to prevent methods from dirtying DB.
 * <p>
 * Example usage: <pre>{@code
 * class SomeDAOTest {
 *     @RegisterExtension
 *     static JdbiExtension db = LedgerPostgresExtension.instance(FOR_EACH_CLASS);
 *
 *     Handle handle = db.getSharedHandle();
 *     SomeDAO someDAO = handle.attach(SomeDAO.class);
 *
 *     ...
 * }
 * }</pre>
 */

@Slf4j
public class LedgerPostgresExtension extends JdbiExternalPostgresExtension implements BeforeAllCallback, AfterAllCallback {

    public final static String DOCKER_IMAGE = "postgres:18.3-alpine";

    public final static String DEFAULT_HOSTNAME = "localhost";
    public final static int DEFAULT_PORT = 5432;
    public final static String DEFAULT_USERNAME = "app";
    public final static String DEFAULT_PASSWORD = "password";

    public final static String EXTERNAL_DB_NAME = "accounting";

    public final static int CODESHIP_POSTGRES_13_PORT = 5435;
    public final static String CODESHIP_DB_NAME = "test";

    public enum Mode {
        //reinitialize DB before every method.
        //pros: provide pristine DB
        //cons: reinitialization takes time
        FOR_EACH_METHOD,

        //reinitialize DB before each class, all method in class will run on same DB
        //pros: faster, no need to reinitialize
        //cons: you have to rollback all changes
        //Important: To use this mode extension must be static!
        FOR_EACH_CLASS
    }


    /**
     * Return standard LedgerPostgresExtension to be used in tests.
     */
    public static JdbiExtension instance(Mode mode) {
        /* from docs:
        PostgreSQL 13 runs on the 5435 port and the credentials are stored in the PGUSER and PGPASSWORD environment variables.
        The default databases created for you are 'development' and 'test'.
         */
        return new LedgerPostgresExtension(
                DEFAULT_HOSTNAME,
                CODESHIP_POSTGRES_13_PORT,
                CODESHIP_DB_NAME,
                Optional.ofNullable(System.getenv("PGUSER"))
                        .filter(StringUtils::isNotBlank)
                        .orElse(DEFAULT_USERNAME),
                Optional.ofNullable(System.getenv("PGPASSWORD"))
                        .filter(StringUtils::isNotBlank)
                        .orElse(DEFAULT_PASSWORD),
                mode
        )
                .withInitializer(new LedgerJdbiLiquibaseMigration(true))
                .withPlugin(new SqlObjectPlugin());

    }


    /**
     * Return JdbiExtension that uses external localhost Postgres server on default port.
     * May be be already initialized, make sure to pass initialize=false in such case.
     * To be used only for development, ex. to run some test on real data.
     * May be be already initialized, make sure to pass initialize=false in such case.
     * Note that this method does not return LedgerPostgresExtension, but some standard JdbiExtension.
     */
    @SuppressWarnings("unused")
    public static JdbiExtension externalPostgresExtension(boolean initialize) {
        return externalPostgresExtension(DEFAULT_HOSTNAME, DEFAULT_PORT, initialize);
    }


    /**
     * Return JdbiExtension that uses  external Postgres server.
     * May be be already initialized, make sure to pass initialize=false in such case.
     * Note that this method does not return LedgerPostgresExtension, but some standard JdbiExtension.
     */
    public static JdbiExtension externalPostgresExtension(String host, int port, boolean initialize) {
        JdbiExtension extension = JdbiExtension.externalPostgres(
                host,
                port,
                EXTERNAL_DB_NAME,
                DEFAULT_USERNAME,
                DEFAULT_PASSWORD
        );
        if (initialize) {
            extension.withInitializer(new LedgerJdbiLiquibaseMigration(true));
        }
        return extension.withPlugin(new SqlObjectPlugin());
    }


    final Mode mode;
    final String host;
    final int port;
    final String database;
    final String username;
    final String password;

    public LedgerPostgresExtension(
            @Nonnull String hostname,
            @Nonnull Integer port,
            @Nonnull String database,
            @Nonnull String username,
            @Nonnull String password,
            @Nonnull Mode mode) {
        super(hostname, port, database, username, password);
        this.mode = mode;
        this.host = hostname;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (mode == FOR_EACH_CLASS) {
            super.afterEach(context);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (mode == FOR_EACH_CLASS) {
            getSharedHandle().rollback();
        }
        if (mode == FOR_EACH_METHOD) {
            super.afterEach(context);
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        startDatabaseInDockerIfNeeded();
        if (mode == FOR_EACH_CLASS) {
            super.beforeEach(context);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        startDatabaseInDockerIfNeeded();
        if (mode == FOR_EACH_METHOD) {
            super.beforeEach(context);
        }
        if (mode == FOR_EACH_CLASS) {
            getSharedHandle().begin();
        }
    }


    private void startDatabaseInDockerIfNeeded() {
        if (isSocketAlive()) {
            //assume it is a postgres, and configured as we expect.
            return;
        }
        ProcessBuilder processBuilder = new ProcessBuilder(
                "docker", "run", "-d", "--rm",
                "-p", "%d:5432".formatted(port),
                "-e", "POSTGRES_PASSWORD=%s".formatted(password),
                "-e", "POSTGRES_USER=%s".formatted(username),
                "-e", "POSTGRES_DB=%s".formatted(database),
                "--name", "postgres_ledger_test",
                DOCKER_IMAGE
        );

        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String stderr = readStream(process.getErrorStream());
                throw new RuntimeException("Failed to run postgres in docker container! Exit code: " + exitCode + ". " + stderr);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to run postgres in docker container!", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to run postgres in docker container!", e);
        }

        waitForDatabase();
    }

    private static String readStream(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes()).trim();
    }

    private void waitForDatabase() {
        final PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setServerNames(new String[]{host});
        ds.setPortNumbers(new int[]{port});
        ds.setUser(username);
        ds.setPassword(password);
        ds.setDatabaseName(database);
        ds.setApplicationName("PostgresHelper");
        ds.setConnectTimeout(500);
        final Jdbi jdbiInstance = Jdbi.create(ds);
        final long start = System.currentTimeMillis();
        while (true) {
            try {
                Handle handle = jdbiInstance.open();
                handle.close();
                return;
            } catch (ConnectionException e) {
                if (System.currentTimeMillis() - start > TimeUnit.SECONDS.toMillis(15)) {
                    log.error("Failed to run postgres in docker container!", e);
                    return;
                }
            }
        }
    }

    public boolean isSocketAlive() {
        SocketAddress socketAddress = new InetSocketAddress(host, port);
        Socket socket = new Socket();
        int timeout = 200;
        try {
            socket.connect(socketAddress, timeout);
            socket.close();
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

}
