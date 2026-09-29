package apps.sarafrika.elimika.shared.tracking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import apps.sarafrika.elimika.shared.tracking.entity.RequestAuditLog;
import apps.sarafrika.elimika.shared.tracking.model.RequestUserMetadata;
import apps.sarafrika.elimika.shared.tracking.repository.RequestAuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;

class RequestAuditServiceTest {

    private final RequestAuditLogRepository repository = mock(RequestAuditLogRepository.class);
    private final RequestUserMetadataResolver resolver = mock(RequestUserMetadataResolver.class);
    private final RequestAuditService service = new RequestAuditService(repository, resolver, new ObjectMapper());

    @Test
    void persistsRedactedQueryString() {
        when(resolver.resolve()).thenReturn(RequestUserMetadata.anonymous("anonymous"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/search");
        request.setQueryString("q=jane+doe&email_like=jane%40x.io&user_uuid_eq=6f1c2d3e-0000-4000-8000-000000000001");

        service.recordRequest(request, 200, 5L, "req-1");

        ArgumentCaptor<RequestAuditLog> captor = ArgumentCaptor.forClass(RequestAuditLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getQueryString())
                .isEqualTo("q=[redacted:8]&email_like=[redacted:9]&user_uuid_eq=6f1c2d3e-0000-4000-8000-000000000001");
        assertThat(captor.getValue().getRequestUri()).isEqualTo("/api/v1/users/search");
    }

    @Test
    void persistsNullQueryStringWhenAbsent() {
        when(resolver.resolve()).thenReturn(RequestUserMetadata.anonymous("anonymous"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/courses");

        service.recordRequest(request, 200, 1L, "req-2");

        ArgumentCaptor<RequestAuditLog> captor = ArgumentCaptor.forClass(RequestAuditLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getQueryString()).isNull();
    }
}
