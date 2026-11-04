package apps.sarafrika.elimika.wallet.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import apps.sarafrika.elimika.testsupport.SharedPostgres;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;

/**
 * The split of existing wallets into role purses, run on a legacy history: new-style creator and delivery legs,
 * legacy class earnings of both kinds, a sale reversal, a withdrawal FIFO spread over two purses and reversed, a
 * held withdrawal, a learner deposit and transfer, and an organisation that must be left alone. The preview script
 * computes the same plan without writing, and an unclassifiable row stops the migration.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Role purse backfill")
class RolePurseBackfillMigrationTest {

    private static final String BEFORE_PURSES = "202611042151";
    private static final String MIGRATION = "src/main/resources/db/migration/V202611042152__split_wallets_into_role_purses.sql";
    private static final String PREVIEW = "scripts/role-purse-backfill-preview.sql";
    private static final String ANN = "a0000000-0000-0000-0000-00000000000a";
    private static final String FAY = "a0000000-0000-0000-0000-00000000000f";
    private static final String ORG = "b0000000-0000-0000-0000-00000000000b";

    private static final SharedPostgres.Database database = SharedPostgres.emptyDatabase();
    private static final Map<String, String> previewBalances = new LinkedHashMap<>();
    private static final List<String> previewRules = new ArrayList<>();

    @BeforeAll
    static void seedTheLegacyHistoryPreviewItThenMigrate() throws Exception {
        flyway(database).target(MigrationVersion.fromVersion(BEFORE_PURSES)).load().migrate();
        try (Connection connection = database.connect(); Statement statement = connection.createStatement()) {
            statement.execute(resource("/wallet/role-purse-backfill-seed.sql"));
        }

        // The preview's plan, inside a transaction that is rolled back, exactly as psql runs it.
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute(planBlock(Files.readString(Path.of(PREVIEW))));
                try (ResultSet rows = statement.executeQuery("select owner_uuid, course_creator, instructor, student, "
                        + "unclassified, cardinality(problems) from role_purse_wallets order by wallet_id")) {
                    while (rows.next()) {
                        previewBalances.put(rows.getString(1), rows.getBigDecimal(2).stripTrailingZeros().toPlainString()
                                + "/" + rows.getBigDecimal(3).stripTrailingZeros().toPlainString()
                                + "/" + rows.getBigDecimal(4).stripTrailingZeros().toPlainString()
                                + "/" + rows.getBigDecimal(5).stripTrailingZeros().toPlainString()
                                + "/" + rows.getInt(6));
                    }
                }
                try (ResultSet rows = statement.executeQuery("select t.reference || ' ' || p.purse || ' ' "
                        + "|| p.amount::numeric(18,0) || ' ' || p.rule from role_purse_plan p "
                        + "join user_wallet_transactions t on t.id = p.txn_id order by t.id, p.portion")) {
                    while (rows.next()) {
                        previewRules.add(rows.getString(1));
                    }
                }
            } finally {
                connection.rollback();
            }
        }

        flyway(database).load().migrate();
    }

    @Test
    @DisplayName("the migration and the preview run the very same plan")
    void previewAndMigrationShareThePlan() throws IOException {
        assertThat(planBlock(Files.readString(Path.of(PREVIEW))))
                .isEqualTo(planBlock(Files.readString(Path.of(MIGRATION))));
    }

    @Test
    @DisplayName("the preview wrote nothing and shows each wallet's purses and the rule behind each legacy row")
    void previewShowsThePlan() {
        assertThat(previewBalances).containsEntry(ANN, "540/860/0/0/0").containsEntry(FAY, "0/0/70/0/0")
                .doesNotContainKey(ORG);
        assertThat(previewRules).contains(
                "o1:l1 INSTRUCTOR 1000 legacy class sale: owner is the class's instructor",
                "o4:l1 COURSE_CREATOR 300 legacy class sale: owner created the class's course or program",
                "o4:l1:reversal COURSE_CREATOR 300 sale reversal: purse of the sale it reverses",
                "WD-2026-000001 INSTRUCTOR 960 withdrawal: oldest outstanding credits first",
                "WD-2026-000001 COURSE_CREATOR 240 withdrawal: oldest outstanding credits first",
                "WD-2026-000001:reversal INSTRUCTOR 960 withdrawal reversal: back to the purses its withdrawal came from",
                "WD-2026-000001:reversal COURSE_CREATOR 240 withdrawal reversal: back to the purses its withdrawal came from",
                "topup-1 STUDENT 100 deposit: learner credit",
                "gift-1 STUDENT 30 transfer out: oldest outstanding credits first");
    }

    @Test
    @DisplayName("each person's balance is split into purses from their own history, summing to what they held")
    void pursesFromHistory() throws SQLException {
        assertThat(query("select purse || ' ' || balance_amount::numeric(18,2) from user_wallets "
                + "where user_uuid = '" + ANN + "' order by purse"))
                .containsExactly("COURSE_CREATOR 540.00", "INSTRUCTOR 860.00");
        assertThat(query("select purse || ' ' || balance_amount::numeric(18,2) from user_wallets "
                + "where user_uuid = '" + FAY + "'")).containsExactly("STUDENT 70.00");
        assertThat(query("select purse || ' ' || balance_amount::numeric(18,2) from user_wallets "
                + "where user_uuid = '" + ORG + "'")).containsExactly("EARNINGS 500.00");
        // The row the person had keeps its uuid and becomes the purse of their oldest row.
        assertThat(query("select purse from user_wallets where id = (select min(id) from user_wallets "
                + "where user_uuid = '" + ANN + "')")).containsExactly("INSTRUCTOR");
    }

    @Test
    @DisplayName("every row lands in its purse with the purse's own running balance, a spread debit as two rows")
    void rowsMoveWithRunningBalances() throws SQLException {
        assertThat(query("select w.purse || ' ' || t.transaction_type || ' ' || t.amount::numeric(18,0) || ' ' "
                + "|| t.balance_before::numeric(18,0) || '>' || t.balance_after::numeric(18,0) "
                + "from user_wallet_transactions t join user_wallets w on w.id = t.wallet_id "
                + "where w.user_uuid = '" + ANN + "' order by w.purse, t.created_date, t.id"))
                .containsExactly(
                        "COURSE_CREATOR SALE 300 0>300",
                        "COURSE_CREATOR SALE_REVERSAL 300 300>0",
                        "COURSE_CREATOR SALE 600 0>600",
                        "COURSE_CREATOR PLATFORM_FEE 60 600>540",
                        "COURSE_CREATOR WITHDRAWAL 240 540>300",
                        "COURSE_CREATOR WITHDRAWAL_REVERSAL 240 300>540",
                        "INSTRUCTOR SALE 1000 0>1000",
                        "INSTRUCTOR SALE 400 1000>1400",
                        "INSTRUCTOR PLATFORM_FEE 40 1400>1360",
                        "INSTRUCTOR WITHDRAWAL 960 1360>400",
                        "INSTRUCTOR WITHDRAWAL_REVERSAL 960 400>1360",
                        "INSTRUCTOR WITHDRAWAL 500 1360>860");
    }

    @Test
    @DisplayName("withdrawals belong to the purse their debit came from; one never debited to the fullest purse")
    void withdrawalsGetTheirPurse() throws SQLException {
        assertThat(query("select reference || ' ' || purse from withdrawals order by reference"))
                .containsExactly("WD-2026-000001 INSTRUCTOR", "WD-2026-000002 INSTRUCTOR", "WD-2026-000003 STUDENT");
    }

    @Test
    @DisplayName("the ledger moves each EARNINGS balance into the purse accounts, balanced, and reconciles per purse")
    void ledgerSplitReconciles() throws SQLException {
        assertThat(query("select la.purse || ' ' || b.posted_amount::numeric(18,2) from ledger_accounts la "
                + "join ledger_account_balances b on b.account_uuid = la.uuid where la.owner_uuid = '" + ANN + "' "
                + "order by la.purse")).containsExactly("COURSE_CREATOR 540.00", "EARNINGS 0.00", "INSTRUCTOR 860.00");
        // Every wallet row against its purse account, as the reconciliation job compares them.
        assertThat(query("""
                select count(*) from user_wallets w
                left join ledger_accounts la on la.owner_type = w.owner_type and la.owner_uuid = w.user_uuid
                    and la.purse = w.purse and la.currency_code = w.currency_code
                where w.balance_amount <> coalesce((select sum(case when e.direction = 'CREDIT' then e.amount
                                                                   else -e.amount end)
                                                    from ledger_entries e where e.account_uuid = la.uuid), 0)
                """)).containsExactly("0");
        assertThat(query("select count(*) from ledger_transactions where idempotency_key like 'wallet-purse-split:%'"))
                .containsExactly("2");
        assertThat(query("select sum(case when direction = 'DEBIT' then amount else -amount end)::numeric(18,2) "
                + "from ledger_entries")).containsExactly("0.00");
        assertThat(query("select updated_values->>'COURSE_CREATOR' || '/' || (updated_values->>'INSTRUCTOR') "
                + "from wallet_audit_events where event_type = 'WALLET_SPLIT_INTO_PURSES' and owner_uuid = '" + ANN + "'"))
                .containsExactly("540.0000/860.0000");
    }

    @Test
    @DisplayName("running the migration again changes nothing")
    void rerunIsANoOp() throws Exception {
        List<String> before = query("select id || ' ' || purse || ' ' || balance_amount from user_wallets order by id");
        try (Connection connection = database.connect(); Statement statement = connection.createStatement()) {
            statement.execute(Files.readString(Path.of(MIGRATION)));
        }
        assertThat(query("select id || ' ' || purse || ' ' || balance_amount from user_wallets order by id"))
                .isEqualTo(before);
        assertThat(query("select count(*) from ledger_transactions where idempotency_key like 'wallet-purse-split:%'"))
                .containsExactly("2");
    }

    @Test
    @DisplayName("a row no rule can classify stops the migration rather than being assigned somewhere")
    void unclassifiableRowAborts() throws Exception {
        SharedPostgres.Database other = SharedPostgres.emptyDatabase();
        flyway(other).target(MigrationVersion.fromVersion(BEFORE_PURSES)).load().migrate();
        try (Connection connection = other.connect(); Statement statement = connection.createStatement()) {
            statement.execute(resource("/wallet/role-purse-backfill-seed.sql"));
            statement.execute("""
                    insert into users (uuid, first_name, last_name, email, user_no, created_by)
                    values ('a0000000-0000-0000-0000-0000000000cc', 'Cy', 'Odd', 'cy@test.local', '900000009', 'seed');
                    select pg_temp.seed_txn('USER', 'a0000000-0000-0000-0000-0000000000cc', 'SALE', 250, 'o9:l1',
                        'Class sale earnings (class d0000000-0000-0000-0000-0000000000b2)', TIMESTAMP '2026-08-05 10:00');
                    """);
        }

        assertThatThrownBy(() -> flyway(other).load().migrate())
                .hasStackTraceContaining("Role purse backfill refused: 1 wallet(s) cannot be split exactly")
                .hasStackTraceContaining("legacy class sale: owner is neither the class's instructor nor its creator");
        try (Connection connection = other.connect(); Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("select count(*) from information_schema.columns "
                     + "where table_name = 'user_wallets' and column_name = 'purse'")) {
            rows.next();
            assertThat(rows.getInt(1)).as("nothing of the migration is left behind").isZero();
        }
    }

    private static List<String> query(String sql) throws SQLException {
        List<String> values = new ArrayList<>();
        try (Connection connection = database.connect(); Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                Object value = rows.getObject(1);
                values.add(value instanceof BigDecimal number ? number.toPlainString() : String.valueOf(value));
            }
        }
        return values;
    }

    /** The text between the plan markers, which must be the same in the migration and the preview. */
    private static String planBlock(String script) {
        String begin = "-- @@role-purse-plan:begin";
        String end = "-- @@role-purse-plan:end";
        return script.substring(script.indexOf(begin), script.indexOf(end) + end.length());
    }

    private static String resource(String path) throws IOException {
        try (InputStream in = RolePurseBackfillMigrationTest.class.getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static FluentConfiguration flyway(SharedPostgres.Database db) {
        return Flyway.configure()
                .dataSource(db.jdbcUrl(), db.username(), db.password())
                .locations("classpath:db/migration")
                .outOfOrder(true);
    }
}
