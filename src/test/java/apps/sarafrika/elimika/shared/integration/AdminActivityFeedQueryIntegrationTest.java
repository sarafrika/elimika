package apps.sarafrika.elimika.shared.integration;

import static org.assertj.core.api.Assertions.assertThat;

import apps.sarafrika.elimika.shared.tracking.entity.RequestAuditLog;
import apps.sarafrika.elimika.shared.tracking.repository.RequestAuditLogRepository;
import apps.sarafrika.elimika.testsupport.SharedPostgres;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

// Each class gets its own database; a cached context must not outlive it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Admin activity feed reads and counts admin requests through the partial index")
class AdminActivityFeedQueryIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SharedPostgres.register(registry);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private RequestAuditLogRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seed() {
        jdbcTemplate.update("DELETE FROM request_audit_log");
        audit("/api/v1/admin/dashboard/statistics", 200, 1);
        audit("/API/V1/ADMIN/courses/pending", 204, 2);
        audit("/api/v1/admin/users", null, 3);
        audit("/api/v1/admin/users", 302, 4);
        audit("/api/v1/admin/users", 403, 5);
        audit("/api/v1/admin/users", 500, 6);
        audit("/api/v1/courses/search", 200, 7);
        audit("/api/v1/administrators", 200, 8);
    }

    @Test
    @DisplayName("rows are admin requests with a null or 2xx-3xx status, newest first")
    void returnsAdminRowsInTheStatusBandNewestFirst() {
        List<RequestAuditLog> rows = repository.findAdminActivity(200, 399,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdDate")));

        // Same prefix semantics as before: case-insensitive, and "/api/v1/administrators" matches too.
        assertThat(rows).extracting(RequestAuditLog::getRequestUri).containsExactly(
                "/api/v1/admin/dashboard/statistics", "/API/V1/ADMIN/courses/pending", "/api/v1/admin/users",
                "/api/v1/admin/users", "/api/v1/administrators");
        assertThat(rows).extracting(RequestAuditLog::getResponseStatus).containsExactly(200, 204, null, 302, 200);
        assertThat(repository.countAdminActivity(200, 399)).isEqualTo(5);
    }

    @Test
    @DisplayName("the count and the page query (as Hibernate renders it) are both served by the partial index")
    void countAndPageUseThePartialIndex() {
        String count = explain("""
                SELECT count(*) FROM request_audit_log
                WHERE lower(request_uri) LIKE '/api/v1/admin%'
                  AND (response_status IS NULL OR (response_status >= 200 AND response_status <= 399))
                """);
        String page = explain("""
                SELECT r.id FROM request_audit_log r
                WHERE lower(r.request_uri) LIKE '/api/v1/admin%' ESCAPE ''
                  AND (r.response_status IS NULL OR (r.response_status >= 200 AND r.response_status <= 399))
                ORDER BY r.created_date DESC FETCH FIRST 12 ROWS ONLY
                """);

        assertThat(count).contains("idx_request_audit_admin_activity");
        assertThat(page).contains("idx_request_audit_admin_activity");
    }

    private String explain(String sql) {
        jdbcTemplate.execute("SET enable_seqscan = off");
        try {
            return String.join("\n", jdbcTemplate.queryForList("EXPLAIN " + sql, String.class));
        } finally {
            jdbcTemplate.execute("RESET enable_seqscan");
        }
    }

    private void audit(String uri, Integer status, int minutesAgo) {
        jdbcTemplate.update("""
                INSERT INTO request_audit_log (request_id, http_method, request_uri, ip_address, response_status,
                                               created_date, created_by, updated_date)
                VALUES (?, 'GET', ?, '10.0.0.1', ?, now() - make_interval(mins => ?), 'test', now())
                """, "req-" + minutesAgo, uri, status, minutesAgo);
    }
}
