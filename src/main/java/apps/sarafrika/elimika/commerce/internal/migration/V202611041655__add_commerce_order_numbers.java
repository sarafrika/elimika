package apps.sarafrika.elimika.commerce.internal.migration;

import apps.sarafrika.elimika.commerce.internal.service.OrderNumbers;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Gives every order a short, unique order number ({@code ELM-K7Q9PX}) from its own sequence.
 * <p>
 * Existing orders are numbered oldest first with the alphabet in {@code COMMERCE_ORDER_NUMBER_ALPHABET},
 * which the running app must also use. Purchases copy the new number so receipts and revenue agree.
 */
public class V202611041655__add_commerce_order_numbers extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws SQLException {
        Connection connection = context.getConnection();
        try (Statement ddl = connection.createStatement()) {
            ddl.execute("CREATE SEQUENCE IF NOT EXISTS commerce_order_number_seq START WITH 1 INCREMENT BY 1");
            ddl.execute("ALTER TABLE commerce_order ADD COLUMN IF NOT EXISTS order_number VARCHAR(16)");
        }

        number(connection, OrderNumbers.fromEnvironment());

        try (Statement ddl = connection.createStatement()) {
            ddl.execute("ALTER TABLE commerce_order ALTER COLUMN order_number SET NOT NULL");
            ddl.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_commerce_order_order_number"
                    + " ON commerce_order (order_number)");
            ddl.execute("COMMENT ON COLUMN commerce_order.order_number IS"
                    + " 'Customer-facing order number and M-Pesa account reference (Sqids of commerce_order_number_seq)'");
            ddl.execute("""
                    UPDATE commerce_purchase p
                       SET order_number = o.order_number
                      FROM commerce_order o
                     WHERE p.order_id = o.uuid::text
                    """);
        }
    }

    private void number(Connection connection, OrderNumbers orderNumbers) throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (Statement select = connection.createStatement();
             ResultSet rows = select.executeQuery(
                     "SELECT id FROM commerce_order WHERE order_number IS NULL ORDER BY id")) {
            while (rows.next()) {
                ids.add(rows.getLong("id"));
            }
        }
        try (PreparedStatement next = connection.prepareStatement("SELECT nextval('commerce_order_number_seq')");
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE commerce_order SET order_number = ? WHERE id = ?")) {
            for (Long id : ids) {
                long sequence;
                try (ResultSet value = next.executeQuery()) {
                    value.next();
                    sequence = value.getLong(1);
                }
                update.setString(1, orderNumbers.format(sequence));
                update.setLong(2, id);
                update.addBatch();
            }
            update.executeBatch();
        }
    }
}
