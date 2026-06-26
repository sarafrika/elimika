package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.authentication.spi.KeycloakAdminEventService;
import apps.sarafrika.elimika.authentication.spi.KeycloakUserService;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorManagementService;
import apps.sarafrika.elimika.shared.spi.analytics.CommerceAnalyticsService;
import apps.sarafrika.elimika.shared.spi.analytics.CourseAnalyticsService;
import apps.sarafrika.elimika.shared.spi.analytics.CourseCreatorAnalyticsService;
import apps.sarafrika.elimika.shared.spi.analytics.InstructorAnalyticsService;
import apps.sarafrika.elimika.shared.spi.analytics.NotificationAnalyticsService;
import apps.sarafrika.elimika.shared.spi.analytics.TimetablingAnalyticsService;
import apps.sarafrika.elimika.shared.tracking.entity.RequestAuditLog;
import apps.sarafrika.elimika.shared.tracking.repository.RequestAuditLogRepository;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.entity.UserOrganisationDomainMapping;
import apps.sarafrika.elimika.tenancy.repository.OrganisationRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainRepository;
import apps.sarafrika.elimika.tenancy.repository.UserOrganisationDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.services.UserNumberService;
import apps.sarafrika.elimika.tenancy.services.UserService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDomainRepository userDomainRepository;

    @Mock
    private UserDomainMappingRepository userDomainMappingRepository;

    @Mock
    private UserOrganisationDomainMappingRepository userOrganisationDomainMappingRepository;

    @Mock
    private OrganisationRepository organisationRepository;

    @Mock
    private UserService userService;

    @Mock
    private UserNumberService userNumberService;

    @Mock
    private InstructorManagementService instructorManagementService;

    @Mock
    private InstructorLookupService instructorLookupService;

    @Mock
    private CourseCreatorLookupService courseCreatorLookupService;

    @Mock
    private CourseAnalyticsService courseAnalyticsService;

    @Mock
    private TimetablingAnalyticsService timetablingAnalyticsService;

    @Mock
    private CommerceAnalyticsService commerceAnalyticsService;

    @Mock
    private NotificationAnalyticsService notificationAnalyticsService;

    @Mock
    private InstructorAnalyticsService instructorAnalyticsService;

    @Mock
    private CourseCreatorAnalyticsService courseCreatorAnalyticsService;

    @Mock
    private KeycloakAdminEventService keycloakAdminEventService;

    @Mock
    private RequestAuditLogRepository requestAuditLogRepository;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private KeycloakUserService keycloakUserService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    void getUserActivityMapsTargetOrganisationAuditEvent() {
        UUID userUuid = UUID.randomUUID();
        UUID organisationUuid = UUID.randomUUID();
        UUID branchUuid = UUID.randomUUID();
        UUID actorUuid = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);

        User user = new User();
        user.setUuid(userUuid);
        when(userRepository.findByUuid(userUuid)).thenReturn(Optional.of(user));
        when(instructorLookupService.findInstructorUuidByUserUuid(userUuid)).thenReturn(Optional.empty());
        when(courseCreatorLookupService.findCourseCreatorUuidByUserUuid(userUuid)).thenReturn(Optional.empty());
        UserOrganisationDomainMapping mapping = new UserOrganisationDomainMapping();
        mapping.setOrganisationUuid(organisationUuid);
        mapping.setBranchUuid(branchUuid);
        when(userOrganisationDomainMappingRepository.findByUserUuid(userUuid)).thenReturn(List.of(mapping));

        RequestAuditLog log = new RequestAuditLog();
        log.setUuid(UUID.randomUUID());
        log.setCreatedDate(LocalDateTime.now());
        log.setHttpMethod("POST");
        log.setRequestUri("/api/v1/admin/organisations/" + organisationUuid + "/moderate");
        log.setQueryString("action=approve");
        log.setResponseStatus(200);
        log.setProcessingTimeMs(35L);
        log.setUserUuid(actorUuid);
        log.setUserFullName("Admin User");
        log.setUserEmail("admin@example.com");
        log.setUserDomains("admin");
        log.setRequestId("req-1");

        when(requestAuditLogRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<RequestAuditLog>>any(),
                any(Pageable.class)
        ))
                .thenReturn(new PageImpl<>(List.of(log), pageable, 1));

        var result = adminService.getUserActivity(userUuid, "all", "organisation", null, pageable);

        assertThat(result.getContent()).hasSize(1);
        var event = result.getContent().getFirst();
        assertThat(event.scope()).isEqualTo("target");
        assertThat(event.category()).isEqualTo("organisation");
        assertThat(event.relatedEntityType()).isEqualTo("organisation");
        assertThat(event.relatedEntityUuid()).isEqualTo(organisationUuid);
        assertThat(event.summary()).isEqualTo("Approved organisation");
    }
}
