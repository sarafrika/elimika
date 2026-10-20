package apps.sarafrika.elimika.testsupport;

import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One PostgreSQL container per test JVM, migrated once into a template database.
 * Each caller gets its own fresh database cloned from that template (or an empty one).
 */
public final class SharedPostgres {

    private static final String TEMPLATE = "elimika_template";
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private static PostgreSQLContainer<?> container;
    private static boolean templateReady;

    private SharedPostgres() {
    }

    /** A database on the shared container. */
    public record Database(String name, String jdbcUrl, String username, String password) {

        public Connection connect() throws SQLException {
            return DriverManager.getConnection(jdbcUrl, username, password);
        }

        public DataSource dataSource() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource(jdbcUrl, username, password);
            dataSource.setDriverClassName("org.postgresql.Driver");
            return dataSource;
        }
    }

    /** Points the Spring datasource at a fresh, fully migrated database. */
    public static void register(DynamicPropertyRegistry registry) {
        Database database = migratedDatabase();
        registry.add("spring.datasource.url", database::jdbcUrl);
        registry.add("spring.datasource.username", database::username);
        registry.add("spring.datasource.password", database::password);
    }

    /** A fresh database with every migration already applied. */
    public static synchronized Database migratedDatabase() {
        if (!templateReady) {
            buildTemplate();
            templateReady = true;
        }
        return create(" TEMPLATE " + TEMPLATE);
    }

    /** A fresh, empty database, for tests that drive Flyway themselves. */
    public static synchronized Database emptyDatabase() {
        return create("");
    }

    private static PostgreSQLContainer<?> container() {
        if (container == null) {
            // Durability is irrelevant for throwaway test data; skipping fsync speeds every write.
            PostgreSQLContainer<?> started = new PostgreSQLContainer<>("postgres:17")
                    .withCommand("postgres", "-c", "fsync=off", "-c", "synchronous_commit=off",
                            "-c", "full_page_writes=off", "-c", "max_connections=500");
            started.start();
            container = started;
        }
        return container;
    }

    private static void buildTemplate() {
        execute("CREATE DATABASE " + TEMPLATE);
        // Same locations and ordering as spring.flyway in application.yaml.
        Flyway.configure()
                .dataSource(url(TEMPLATE), container().getUsername(), container().getPassword())
                .locations("classpath:db/migration", "classpath:apps/sarafrika/elimika/shared/tracking/migration")
                .outOfOrder(true)
                .load()
                .migrate();
        // A template cannot be cloned while anyone is connected to it.
        execute("SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '" + TEMPLATE + "'");
        execute("ALTER DATABASE " + TEMPLATE + " WITH IS_TEMPLATE true ALLOW_CONNECTIONS false");
    }

    private static Database create(String clause) {
        String name = "elimika_test_" + SEQUENCE.incrementAndGet();
        execute("CREATE DATABASE " + name + clause);
        return new Database(name, url(name), container().getUsername(), container().getPassword());
    }

    private static String url(String database) {
        PostgreSQLContainer<?> postgres = container();
        return "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/" + database;
    }

    private static void execute(String sql) {
        PostgreSQLContainer<?> postgres = container();
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not run on the shared test database: " + sql, e);
        }
    }
}
