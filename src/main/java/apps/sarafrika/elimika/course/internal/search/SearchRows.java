package apps.sarafrika.elimika.course.internal.search;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/** Row-reading helpers shared by the course module's search sources. */
final class SearchRows {

    static final int MAX_TEXT_LENGTH = 2000;

    private SearchRows() {
    }

    static String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= MAX_TEXT_LENGTH ? text : text.substring(0, MAX_TEXT_LENGTH);
    }

    static UUID uuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    static boolean flag(ResultSet rs, String column) throws SQLException {
        return rs.getBoolean(column);
    }

    /** Reads a {@code bigint} column such as {@code EXTRACT(EPOCH FROM created_date)::bigint}. */
    static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    static boolean isFree(BigDecimal price) {
        return price == null || price.signum() == 0;
    }
}
