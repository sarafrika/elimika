package apps.sarafrika.elimika.tenancy.controller;

import apps.sarafrika.elimika.shared.enums.Gender;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.security.UserContactSecurityService;
import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.service.MediaServeService;
import apps.sarafrika.elimika.shared.tracking.service.RequestAuditService;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.dto.UserSummaryDTO;
import apps.sarafrika.elimika.tenancy.services.UserService;
import apps.sarafrika.elimika.tenancy.spi.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the users routes that decide what a caller may learn about somebody else: the self-scoped
 * identity route that replaced the sign-in bootstrap, the general search that could only be closed
 * once it existed, the batch directory, and the single-record route that now answers with the
 * directory projection unless the caller has a claim to the person's contact details.
 * <p>
 * The filter chain here permits every request on purpose, so the only thing that can refuse one is
 * the {@code @PreAuthorize} on the handler. Production additionally refuses anonymous callers at the
 * chain (its baseline is {@code anyRequest().authenticated()}, which answers 401 rather than 403) —
 * that outer layer is not what these tests are for. What they prove is that the annotations
 * themselves hold if a request ever reaches the controller.
 */
@WebMvcTest(value = UserController.class, properties = "app.keycloak.realm=test-realm")
@ExtendWith(SpringExtension.class)
@Import({UserControllerTest.MockConfig.class, UserControllerTest.MethodSecurityConfig.class})
@DisplayName("User identity and search authorization")
class UserControllerTest {

    private static final UUID CALLER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private DomainSecurityService domainSecurityService;

    @Autowired
    private UserContactSecurityService userContactSecurityService;

    @BeforeEach
    void setUp() {
        reset(userService, domainSecurityService, userContactSecurityService);
    }

    // ================================
    // GET /api/v1/users/me
    // ================================

    @Test
    @DisplayName("resolves the caller from the token, not from a query parameter")
    void currentUserIsResolvedFromTheToken() throws Exception {
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(CALLER_UUID);
        when(userService.getUserByUuid(CALLER_UUID)).thenReturn(user(CALLER_UUID, List.of("student")));

        mockMvc.perform(get("/api/v1/users/me").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uuid").value(CALLER_UUID.toString()))
                .andExpect(jsonPath("$.data.email").value("jane.doe@example.com"))
                .andExpect(jsonPath("$.data.user_domain[0]").value("student"));

        verify(userService).getUserByUuid(CALLER_UUID);
        verify(userService, never()).search(any(), any());
    }

    @Test
    @DisplayName("carries the caller's domains, so the client needs no second call")
    void currentUserCarriesDomains() throws Exception {
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(CALLER_UUID);
        when(userService.getUserByUuid(CALLER_UUID))
                .thenReturn(user(CALLER_UUID, List.of("instructor", "organisation_user")));

        mockMvc.perform(get("/api/v1/users/me").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user_domain.length()").value(2));
    }

    @Test
    @DisplayName("a brand-new account with no domains still gets an answer")
    void currentUserWithNoDomainsIsStillReturned() throws Exception {
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(CALLER_UUID);
        when(userService.getUserByUuid(CALLER_UUID)).thenReturn(user(CALLER_UUID, List.of()));

        mockMvc.perform(get("/api/v1/users/me").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uuid").value(CALLER_UUID.toString()))
                .andExpect(jsonPath("$.data.user_domain.length()").value(0));
    }

    @Test
    @DisplayName("refuses an unauthenticated caller")
    void currentUserRefusesUnauthenticatedCaller() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isForbidden());

        verify(userService, never()).getUserByUuid(any());
    }

    @Test
    @DisplayName("answers 404 when the token authenticates but no local record answers to it")
    void currentUserWithoutLocalRecordIsNotFound() throws Exception {
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(null);

        mockMvc.perform(get("/api/v1/users/me").with(jwt()))
                .andExpect(status().isNotFound());

        verify(userService, never()).getUserByUuid(any());
    }

    // ================================
    // GET /api/v1/users/search
    // ================================

    @Test
    @DisplayName("search refuses a caller who is not a platform admin")
    void searchRefusesNonPlatformAdmin() throws Exception {
        when(domainSecurityService.isPlatformAdmin()).thenReturn(false);

        mockMvc.perform(get("/api/v1/users/search")
                        .param("email_eq", "jane.doe@example.com")
                        .with(jwt()))
                .andExpect(status().isForbidden());

        verify(userService, never()).search(any(), any());
    }

    @Test
    @DisplayName("search refuses an unauthenticated caller")
    void searchRefusesUnauthenticatedCaller() throws Exception {
        mockMvc.perform(get("/api/v1/users/search").param("email_eq", "jane.doe@example.com"))
                .andExpect(status().isForbidden());

        verify(userService, never()).search(any(), any());
    }

    @Test
    @DisplayName("search still serves a platform admin")
    void searchServesPlatformAdmin() throws Exception {
        when(domainSecurityService.isPlatformAdmin()).thenReturn(true);
        when(userService.search(any(), any()))
                .thenReturn(new PageImpl<>(List.of(user(CALLER_UUID, List.of("student"))), PageRequest.of(0, 20), 1L));

        mockMvc.perform(get("/api/v1/users/search")
                        .param("email_eq", "jane.doe@example.com")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].uuid").value(CALLER_UUID.toString()));
    }

    // ================================
    // GET /api/v1/users/{uuid}
    // ================================

    /**
     * The leak this route existed to close: any signed-in account could read anybody's email, phone
     * number and date of birth just by holding their UUID, which every roster hands out.
     */
    @Test
    @DisplayName("a caller with no claim to the person gets the directory projection, not the record")
    void userByUuidWithholdsContactDetailsFromUnrelatedCaller() throws Exception {
        UUID target = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(userContactSecurityService.canReadContactDetails(target)).thenReturn(false);
        when(userService.getUserDirectory(List.of(target))).thenReturn(List.of(summary(target, "Jane", "Doe")));

        mockMvc.perform(get("/api/v1/users/" + target).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uuid").value(target.toString()))
                .andExpect(jsonPath("$.data.full_name").value("Jane A. Doe"))
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.phone_number").doesNotExist())
                .andExpect(jsonPath("$.data.dob").doesNotExist())
                .andExpect(jsonPath("$.data.username").doesNotExist())
                .andExpect(jsonPath("$.data.keycloak_id").doesNotExist());

        verify(userService, never()).getUserByUuid(any());
    }

    /**
     * The other half: withholding contact details from everyone would blank out the instructor's
     * student list, the waiting list and the organisation roster, so a caller the contact predicate
     * accepts must still get the whole record.
     */
    @Test
    @DisplayName("a caller with a claim to the person still gets the full record")
    void userByUuidServesTheRecordToAPrivilegedCaller() throws Exception {
        UUID target = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(userContactSecurityService.canReadContactDetails(target)).thenReturn(true);
        when(userService.getUserByUuid(target)).thenReturn(user(target, List.of("student")));

        mockMvc.perform(get("/api/v1/users/" + target).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uuid").value(target.toString()))
                .andExpect(jsonPath("$.data.email").value("jane.doe@example.com"))
                .andExpect(jsonPath("$.data.dob").value("1990-01-01"));

        verify(userService, never()).getUserDirectory(any());
    }

    /**
     * The payload shape is chosen by the contact predicate alone. Nothing else in the handler may
     * decide it, or the two branches drift apart.
     */
    @Test
    @DisplayName("the payload shape follows the contact predicate, which is asked for the subject")
    void userByUuidAsksTheContactPredicateForTheSubject() throws Exception {
        UUID target = UUID.fromString("33333333-3333-3333-3333-333333333333");
        when(userContactSecurityService.canReadContactDetails(target)).thenReturn(false);
        when(userService.getUserDirectory(List.of(target))).thenReturn(List.of(summary(target, "John", "Smith")));

        mockMvc.perform(get("/api/v1/users/" + target).with(jwt()))
                .andExpect(status().isOk());

        verify(userContactSecurityService).canReadContactDetails(target);
    }

    /**
     * A UUID that resolves to nothing must answer 404 on the summary branch too, rather than an
     * empty 200 that a client would render as a blank person.
     */
    @Test
    @DisplayName("answers 404 on the summary branch when the account does not exist")
    void userByUuidIsNotFoundOnTheSummaryBranch() throws Exception {
        UUID missing = UUID.fromString("44444444-4444-4444-4444-444444444444");
        when(userContactSecurityService.canReadContactDetails(missing)).thenReturn(false);
        when(userService.getUserDirectory(List.of(missing))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/users/" + missing).with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("refuses an unauthenticated caller")
    void userByUuidRefusesUnauthenticatedCaller() throws Exception {
        mockMvc.perform(get("/api/v1/users/22222222-2222-2222-2222-222222222222"))
                .andExpect(status().isForbidden());

        verify(userService, never()).getUserByUuid(any());
        verify(userService, never()).getUserDirectory(any());
    }

    // ================================
    // GET /api/v1/users/directory
    // ================================

    @Test
    @DisplayName("directory returns exactly the users asked for")
    void directoryReturnsOnlyTheRequestedUsers() throws Exception {
        UUID first = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID second = UUID.fromString("33333333-3333-3333-3333-333333333333");
        when(userService.getUserDirectory(List.of(first, second)))
                .thenReturn(List.of(summary(first, "Jane", "Doe"), summary(second, "John", "Smith")));

        mockMvc.perform(get("/api/v1/users/directory")
                        .param("uuid_in", first + "," + second)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].uuid").value(first.toString()))
                .andExpect(jsonPath("$.data[1].uuid").value(second.toString()));

        verify(userService).getUserDirectory(List.of(first, second));
        verify(userService, never()).search(any(), any());
    }

    @Test
    @DisplayName("directory carries display identity and withholds contact details")
    void directoryOmitsContactDetails() throws Exception {
        UUID uuid = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(userService.getUserDirectory(List.of(uuid))).thenReturn(List.of(summary(uuid, "Jane", "Doe")));

        mockMvc.perform(get("/api/v1/users/directory").param("uuid_in", uuid.toString()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].full_name").value("Jane A. Doe"))
                .andExpect(jsonPath("$.data[0].display_name").value("Jane Doe"))
                .andExpect(jsonPath("$.data[0].user_no").value("000000001"))
                .andExpect(jsonPath("$.data[0].profile_image_url").value("https://cdn.example.com/jane.png"))
                .andExpect(jsonPath("$.data[0].email").doesNotExist())
                .andExpect(jsonPath("$.data[0].phone_number").doesNotExist())
                .andExpect(jsonPath("$.data[0].dob").doesNotExist());
    }

    /**
     * A roster assembled from foreign keys must not fail wholesale because one referenced account
     * has since been deleted, so an unmatched UUID is simply absent from the response.
     */
    @Test
    @DisplayName("directory skips unknown uuids instead of failing the request")
    void directorySkipsUnknownUuids() throws Exception {
        UUID known = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID unknown = UUID.fromString("44444444-4444-4444-4444-444444444444");
        when(userService.getUserDirectory(List.of(known, unknown)))
                .thenReturn(List.of(summary(known, "Jane", "Doe")));

        mockMvc.perform(get("/api/v1/users/directory")
                        .param("uuid_in", known + "," + unknown)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].uuid").value(known.toString()));
    }

    @Test
    @DisplayName("directory refuses more uuids than the cap rather than truncating")
    void directoryEnforcesTheCap() throws Exception {
        String tooMany = IntStream.rangeClosed(0, UserController.MAX_DIRECTORY_UUIDS)
                .mapToObj(index -> UUID.randomUUID().toString())
                .collect(Collectors.joining(","));

        mockMvc.perform(get("/api/v1/users/directory").param("uuid_in", tooMany).with(jwt()))
                .andExpect(status().isBadRequest());

        verify(userService, never()).getUserDirectory(any());
    }

    @Test
    @DisplayName("directory serves a request sitting exactly on the cap")
    void directoryAcceptsTheCapExactly() throws Exception {
        String atCap = IntStream.range(0, UserController.MAX_DIRECTORY_UUIDS)
                .mapToObj(index -> UUID.randomUUID().toString())
                .collect(Collectors.joining(","));
        when(userService.getUserDirectory(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/users/directory").param("uuid_in", atCap).with(jwt()))
                .andExpect(status().isOk());

        verify(userService).getUserDirectory(any());
    }

    @Test
    @DisplayName("directory refuses an unauthenticated caller")
    void directoryRefusesUnauthenticatedCaller() throws Exception {
        mockMvc.perform(get("/api/v1/users/directory")
                        .param("uuid_in", "22222222-2222-2222-2222-222222222222"))
                .andExpect(status().isForbidden());

        verify(userService, never()).getUserDirectory(any());
    }

    /**
     * The directory is a people directory, not a private record: an ordinary learner resolving the
     * names on a class roster is exactly the intended caller, so a non-admin must get through.
     */
    @Test
    @DisplayName("directory serves an ordinary authenticated caller, not just admins")
    void directoryServesNonAdmin() throws Exception {
        UUID uuid = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(domainSecurityService.isPlatformAdmin()).thenReturn(false);
        when(userService.getUserDirectory(List.of(uuid))).thenReturn(List.of(summary(uuid, "Jane", "Doe")));

        mockMvc.perform(get("/api/v1/users/directory").param("uuid_in", uuid.toString()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].uuid").value(uuid.toString()));
    }

    private static UserSummaryDTO summary(UUID uuid, String firstName, String lastName) {
        return new UserSummaryDTO(uuid, "000000001", firstName, "A.", lastName,
                "https://cdn.example.com/jane.png", Gender.FEMALE);
    }

    private static UserDTO user(UUID uuid, List<String> domains) {
        return new UserDTO(
                uuid, "000000001", "Jane", null, "Doe", "jane.doe@example.com", "janedoe",
                null, LocalDate.of(1990, 1, 1), null, true, "kc-123",
                null, null, "system", null, null, domains, List.of());
    }

    /**
     * Method security is switched on by {@code SecurityConfiguration}, which a web slice does not
     * load. The chain permits everything so that the handler annotations are the only gate under
     * test.
     */
    @EnableMethodSecurity(securedEnabled = true)
    static class MethodSecurityConfig {
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                    .build();
        }
    }

    static class MockConfig {
        @Bean
        UserService userService() {
            return Mockito.mock(UserService.class);
        }

        @Bean
        DomainSecurityService domainSecurityService() {
            return Mockito.mock(DomainSecurityService.class);
        }

        @Bean
        UserContactSecurityService userContactSecurityService() {
            return Mockito.mock(UserContactSecurityService.class);
        }

        @Bean
        MediaServeService mediaServeService() {
            return Mockito.mock(MediaServeService.class);
        }

        @Bean
        StorageProperties storageProperties() {
            return Mockito.mock(StorageProperties.class);
        }

        @Bean
        RequestAuditService requestAuditService() {
            return Mockito.mock(RequestAuditService.class);
        }

        @Bean
        UserManagementService userManagementService() {
            return Mockito.mock(UserManagementService.class);
        }
    }
}
