package apps.sarafrika.elimika.shared.tracking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import apps.sarafrika.elimika.shared.tracking.model.RequestAuditEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;

class RequestAuditServiceTest {

    private final RequestAuditWriter writer = mock(RequestAuditWriter.class);
    private final RequestAuditService service = new RequestAuditService(writer, new ObjectMapper());

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void enqueuesRedactedQueryString() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/search");
        request.setQueryString("q=jane+doe&email_like=jane%40x.io&user_uuid_eq=6f1c2d3e-0000-4000-8000-000000000001");

        service.recordRequest(request, 200, 5L, "req-1");

        ArgumentCaptor<RequestAuditEntry> captor = ArgumentCaptor.forClass(RequestAuditEntry.class);
        verify(writer).offer(captor.capture());
        assertThat(captor.getValue().queryString())
                .isEqualTo("q=[redacted:8]&email_like=[redacted:9]&user_uuid_eq=6f1c2d3e-0000-4000-8000-000000000001");
        assertThat(captor.getValue().requestUri()).isEqualTo("/api/v1/users/search");
        assertThat(captor.getValue().createdBy()).isEqualTo("SYSTEM");
        assertThat(captor.getValue().keycloakId()).isNull();
    }

    @Test
    void enqueuesNullQueryStringWhenAbsent() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/courses");

        service.recordRequest(request, 200, 1L, "req-2");

        ArgumentCaptor<RequestAuditEntry> captor = ArgumentCaptor.forClass(RequestAuditEntry.class);
        verify(writer).offer(captor.capture());
        assertThat(captor.getValue().queryString()).isNull();
    }
}
