package db.migration;

import apps.sarafrika.elimika.shared.tracking.QueryStringRedactor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * One-off: applies {@link QueryStringRedactor} to the {@code query_string} of every existing
 * {@code request_audit_log} row, so search text, coordinates and e-mail addresses captured before the
 * redactor existed are masked as {@code [redacted:<length>]}.
 * <p>
 * Walks the table by id in batches of {@value #BATCH_SIZE} and updates only {@code query_string},
 * and only where redaction changes it. Actor, URI, status, timestamps (including
 * {@code updated_date}) and every other column are left as they were. The redactor is a fixed point,
 * so re-running over already redacted rows changes nothing.
 */
public class V202609301821__redact_historical_audit_query_strings extends BaseJavaMigration {

    static final int BATCH_SIZE = 1000;

    private static final String SELECT_BATCH = """
            SELECT id, query_string FROM request_audit_log
            WHERE id > ? AND query_string IS NOT NULL AND query_string <> ''
            ORDER BY id
            LIMIT ?
            """;
    private static final String UPDATE_ROW = "UPDATE request_audit_log SET query_string = ? WHERE id = ?";

    @Override
    public void migrate(Context context) throws SQLException {
        redactAll(context.getConnection());
    }

    /** Redacts every row; returns how many rows were changed. */
    public static long redactAll(Connection connection) throws SQLException {
        long lastId = 0;
        long changed = 0;
        try (PreparedStatement select = connection.prepareStatement(SELECT_BATCH);
             PreparedStatement update = connection.prepareStatement(UPDATE_ROW)) {
            while (true) {
                select.setLong(1, lastId);
                select.setInt(2, BATCH_SIZE);
                int seen = 0;
                int pending = 0;
                try (ResultSet rows = select.executeQuery()) {
                    while (rows.next()) {
                        seen++;
                        lastId = rows.getLong("id");
                        String original = rows.getString("query_string");
                        String redacted = QueryStringRedactor.redact(original);
                        if (!Objects.equals(original, redacted)) {
                            update.setString(1, redacted);
                            update.setLong(2, lastId);
                            update.addBatch();
                            pending++;
                        }
                    }
                }
                if (pending > 0) {
                    update.executeBatch();
                    changed += pending;
                }
                if (seen < BATCH_SIZE) {
                    return changed;
                }
            }
        }
    }
}
