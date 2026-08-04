package apps.sarafrika.elimika.tenancy.integration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The skills fund migration, run for real against PostgreSQL, with legacy rows already in the table.
 * <p>
 * Everything asserted here is invisible to a mocked test. The status mapping only matters if rows
 * written under the old free-form column survive it; the CHECK constraints and the currency foreign
 * key only exist in the database; and the fail-loud behaviour on an unmappable status is a
 * {@code RAISE EXCEPTION} inside a {@code DO} block that Java never sees.
 * <p>
 * Each scenario gets its own database on one container, and migrations are run in two passes —
 * up to the version immediately preceding the skills fund change, then the rest — so that the legacy
 * rows exist at the moment the migration that has to cope with them runs.
 */
@Testcontainers
@DisplayName("Skills fund schema migration against PostgreSQL")
class SkillsFundSchemaIntegrationTest {

    /** Matches the script this test exists to exercise, without pinning its timestamp. */
    private static final String MIGRATION_MARKER = "harden_skills_fund_money_schema";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static MigrationVersion previousVersion;

    private static JdbcTemplate migratedFund;
    private static UUID organisationUuid;
    private static Map<String, UUID> legacyTransactions;

    @BeforeAll
    static void migrateWithLegacyRowsInPlace() {
        resolveVersions();

        DataSource dataSource = freshDatabase("skills_fund_legacy");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        flyway(dataSource).migrate();

        organisationUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO organisation (uuid, name, created_by) VALUES (?, ?, 'test')",
                organisationUuid, "Legacy Academy");
        insertLegacySource(jdbc, "Government Grant", "1000.00");

        // Every status the legacy code could produce, in the casing it produced it in, plus the two
        // spellings that meant the same thing.
        legacyTransactions = Map.of(
                "Pending", insertLegacyTransaction(jdbc, "Pending", "10.00"),
                "Allocated", insertLegacyTransaction(jdbc, "Allocated", "20.00"),
                "Approved", insertLegacyTransaction(jdbc, "Approved", "30.00"),
                "Completed", insertLegacyTransaction(jdbc, "Completed", "40.00"),
                "Disbursed", insertLegacyTransaction(jdbc, "Disbursed", "50.00"),
                "  pending  ", insertLegacyTransaction(jdbc, "  pending  ", "60.00"));

        flywayThrough(dataSource, null).migrate();
        migratedFund = jdbc;
    }

    @Test
    @DisplayName("every legacy status lands on the enum value the code expects")
    void legacyStatusesAreMapped() {
        assertThat(statusOf("Pending")).isEqualTo("PENDING");
        assertThat(statusOf("Allocated")).isEqualTo("ALLOCATED");
        assertThat(statusOf("Approved")).isEqualTo("APPROVED");
        assertThat(statusOf("Completed")).isEqualTo("DISBURSED");
        assertThat(statusOf("Disbursed")).isEqualTo("DISBURSED");
    }

    @Test
    @DisplayName("a hand-edited status with stray casing and whitespace is normalised, not dropped")
    void messyLegacyValuesSurvive() {
        assertThat(statusOf("  pending  ")).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("nothing is lost in the migration — every legacy row is still there")
    void noRowIsDropped() {
        Integer count = migratedFund.queryForObject(
                "SELECT COUNT(*) FROM skills_fund_transactions "
                        + "WHERE organisation_uuid = ? AND description = 'Legacy movement'",
                Integer.class, organisationUuid);
        assertThat(count).isEqualTo(legacyTransactions.size());
    }

    @Test
    @DisplayName("the backfill declares pre-existing rows to be KES on both tables")
    void currencyIsBackfilledToKes() {
        assertThat(migratedFund.queryForList(
                "SELECT DISTINCT currency_code FROM skills_fund_transactions", String.class))
                .containsExactly("KES");
        assertThat(migratedFund.queryForList(
                "SELECT DISTINCT currency_code FROM skills_fund_sources", String.class))
                .containsExactly("KES");
    }

    @Test
    @DisplayName("a currency cannot be omitted on a new row")
    void currencyIsRequired() {
        assertThatThrownBy(() -> migratedFund.update(
                "INSERT INTO skills_fund_sources (organisation_uuid, name, amount) VALUES (?, 'No currency', 1)",
                organisationUuid))
                .hasMessageContaining("currency_code");

        assertThatThrownBy(() -> migratedFund.update(
                "INSERT INTO skills_fund_transactions (organisation_uuid, amount, status) VALUES (?, 1, 'PENDING')",
                organisationUuid))
                .hasMessageContaining("currency_code");
    }

    @Test
    @DisplayName("a currency the platform does not know is refused by the foreign key")
    void unknownCurrencyIsRefused() {
        assertThatThrownBy(() -> migratedFund.update(
                "INSERT INTO skills_fund_sources (organisation_uuid, name, amount, currency_code) "
                        + "VALUES (?, 'Bad currency', 1, 'ZZZ')", organisationUuid))
                .hasMessageContaining("fk_skills_fund_sources_currency");
    }

    @Test
    @DisplayName("the CHECK constraint refuses a status outside the closed set")
    void checkConstraintRefusesUnknownStatuses() {
        assertThatThrownBy(() -> insertTransaction("Completed"))
                .hasMessageContaining("chk_skills_fund_transactions_status");
        assertThatThrownBy(() -> insertTransaction("pending"))
                .hasMessageContaining("chk_skills_fund_transactions_status");
        assertThatThrownBy(() -> insertTransaction("Whatever"))
                .hasMessageContaining("chk_skills_fund_transactions_status");
    }

    @Test
    @DisplayName("the four enum values are all accepted, and the column defaults to PENDING")
    void theEnumValuesAreAccepted() {
        for (String status : List.of("PENDING", "ALLOCATED", "APPROVED", "DISBURSED")) {
            insertTransaction(status);
        }

        migratedFund.update("INSERT INTO skills_fund_transactions (organisation_uuid, amount, currency_code) "
                + "VALUES (?, 1, 'KES')", organisationUuid);
        assertThat(migratedFund.queryForObject(
                "SELECT status FROM skills_fund_transactions ORDER BY id DESC LIMIT 1", String.class))
                .isEqualTo("PENDING");
    }

    @Test
    @DisplayName("legacy rows keep their display name and gain no invented beneficiary")
    void beneficiaryIsNeverBackfilled() {
        List<Map<String, Object>> rows = migratedFund.queryForList(
                "SELECT target_name, beneficiary_user_uuid FROM skills_fund_transactions "
                        + "WHERE organisation_uuid = ? AND description = 'Legacy movement'",
                organisationUuid);

        assertThat(rows).isNotEmpty();
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.get("target_name")).isEqualTo("John Doe");
            assertThat(row.get("beneficiary_user_uuid")).isNull();
        });
    }

    @Test
    @DisplayName("a beneficiary must name a real user, and outlives that user as a null rather than a deletion")
    void beneficiaryIsAForeignKeyThatSetsNull() {
        assertThatThrownBy(() -> migratedFund.update(
                "INSERT INTO skills_fund_transactions "
                        + "(organisation_uuid, amount, currency_code, status, beneficiary_user_uuid) "
                        + "VALUES (?, 1, 'KES', 'PENDING', ?)", organisationUuid, UUID.randomUUID()))
                .hasMessageContaining("beneficiary_user_uuid");

        UUID learner = insertUser();
        UUID txnUuid = UUID.randomUUID();
        migratedFund.update("INSERT INTO skills_fund_transactions "
                        + "(uuid, organisation_uuid, amount, currency_code, status, beneficiary_user_uuid) "
                        + "VALUES (?, ?, 1, 'KES', 'DISBURSED', ?)", txnUuid, organisationUuid, learner);

        migratedFund.update("DELETE FROM users WHERE uuid = ?", learner);

        Map<String, Object> row = migratedFund.queryForMap(
                "SELECT amount, beneficiary_user_uuid FROM skills_fund_transactions WHERE uuid = ?", txnUuid);
        assertThat(row.get("beneficiary_user_uuid")).isNull();
        assertThat(row).containsKey("amount");
    }

    @Test
    @DisplayName("sources gain a deleted flag that defaults to live")
    void sourcesAreSoftDeletable() {
        assertThat(migratedFund.queryForList(
                "SELECT deleted FROM skills_fund_sources WHERE organisation_uuid = ?", Boolean.class, organisationUuid))
                .containsOnly(false);
    }

    @Test
    @DisplayName("a status that maps to nothing aborts the migration instead of being quietly rewritten")
    void anUnmappableStatusAbortsTheMigration() {
        DataSource dataSource = freshDatabase("skills_fund_stray");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        flyway(dataSource).migrate();

        UUID strayOrganisation = UUID.randomUUID();
        jdbc.update("INSERT INTO organisation (uuid, name, created_by) VALUES (?, ?, 'test')",
                strayOrganisation, "Stray Academy");
        jdbc.update("INSERT INTO skills_fund_transactions (uuid, organisation_uuid, amount, status) "
                        + "VALUES (?, ?, 99, 'Reimbursed')", UUID.randomUUID(), strayOrganisation);

        assertThatThrownBy(() -> flywayThrough(dataSource, null).migrate())
                .rootCause()
                .hasMessageContaining("Reimbursed")
                .hasMessageContaining("no mapping to the new enum");

        // And the damage is contained: the row is untouched, so it can be corrected and the
        // migration re-run rather than investigated from a backup.
        assertThat(jdbc.queryForObject(
                "SELECT status FROM skills_fund_transactions WHERE organisation_uuid = ?",
                String.class, strayOrganisation))
                .isEqualTo("Reimbursed");
    }

    // ---------------------------------------------------------------------------------------------

    private String statusOf(String legacyValue) {
        return migratedFund.queryForObject(
                "SELECT status FROM skills_fund_transactions WHERE uuid = ?",
                String.class, legacyTransactions.get(legacyValue));
    }

    private void insertTransaction(String status) {
        migratedFund.update("INSERT INTO skills_fund_transactions "
                        + "(organisation_uuid, amount, currency_code, status) VALUES (?, 1, 'KES', ?)",
                organisationUuid, status);
    }

    private UUID insertUser() {
        UUID uuid = UUID.randomUUID();
        migratedFund.update("INSERT INTO users (uuid, first_name, last_name, email, user_no, created_by) "
                        + "VALUES (?, 'Fund', 'Learner', ?, lpad(nextval('user_no_seq')::text, 9, '0'), 'test')",
                uuid, "u" + Long.toHexString(System.nanoTime()) + "@example.test");
        return uuid;
    }

    private static void insertLegacySource(JdbcTemplate jdbc, String name, String amount) {
        jdbc.update("INSERT INTO skills_fund_sources (uuid, organisation_uuid, name, source_type, amount) "
                        + "VALUES (?, ?, ?, 'Government Grant', ?::numeric)",
                UUID.randomUUID(), organisationUuid, name, amount);
    }

    private static UUID insertLegacyTransaction(JdbcTemplate jdbc, String status, String amount) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO skills_fund_transactions "
                        + "(uuid, organisation_uuid, description, target_name, amount, transaction_type, status) "
                        + "VALUES (?, ?, 'Legacy movement', 'John Doe', ?::numeric, 'Allocation', ?)",
                uuid, organisationUuid, amount, status);
        return uuid;
    }

    /**
     * Locates the skills fund migration and the one immediately before it, so the staged run does not
     * have to hard-code a timestamp that other work in flight will keep moving.
     */
    private static void resolveVersions() {
        MigrationInfo[] all = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .load()
                .info()
                .all();

        int index = -1;
        for (int i = 0; i < all.length; i++) {
            if (all[i].getScript() != null && all[i].getScript().contains(MIGRATION_MARKER)) {
                index = i;
                break;
            }
        }
        assertThat(index)
                .withFailMessage("No migration matching '%s' found among %s",
                        MIGRATION_MARKER, Arrays.toString(all))
                .isGreaterThan(0);

        previousVersion = all[index - 1].getVersion();
    }

    /** Migrations up to, but not including, the skills fund change. */
    private static Flyway flyway(DataSource dataSource) {
        return flywayThrough(dataSource, previousVersion);
    }

    private static Flyway flywayThrough(DataSource dataSource, MigrationVersion target) {
        var configuration = Flyway.configure()
                .dataSource(dataSource)
                .outOfOrder(true);
        if (target != null) {
            configuration = configuration.target(target);
        }
        return configuration.load();
    }

    private static DataSource freshDatabase(String name) {
        try (Connection connection = postgres.createConnection("");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + name);
        } catch (Exception e) {
            throw new IllegalStateException("Could not create test database " + name, e);
        }

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(postgres.getDriverClassName());
        dataSource.setUrl("jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/" + name);
        dataSource.setUsername(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());
        return dataSource;
    }
}
