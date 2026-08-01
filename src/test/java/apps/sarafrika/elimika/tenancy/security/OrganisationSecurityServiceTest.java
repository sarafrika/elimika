package apps.sarafrika.elimika.tenancy.security;

import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.security.RequestScopedCache;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.entity.StudentGroup;
import apps.sarafrika.elimika.tenancy.entity.TrainingBranch;
import apps.sarafrika.elimika.tenancy.repository.StudentGroupRepository;
import apps.sarafrika.elimika.tenancy.repository.TrainingBranchRepository;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards tenant isolation on the organisation endpoints.
 * <p>
 * The rule under test is that authority is scoped to the organisation named in the request:
 * managing organisation A must grant nothing at all over organisation B. Before these predicates
 * existed any authenticated caller could act on any organisation — including granting themselves
 * the {@code admin} domain in one they had never joined.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrganisationSecurityServiceTest {

    private static final String KEYCLOAK_ID = "keycloak-subject";
    private static final UUID CALLER_UUID = UUID.randomUUID();
    private static final UUID ORG_UUID = UUID.randomUUID();
    private static final UUID OTHER_ORG_UUID = UUID.randomUUID();
    private static final UUID GROUP_UUID = UUID.randomUUID();
    private static final UUID BRANCH_UUID = UUID.randomUUID();

    @Mock private StudentGroupRepository studentGroupRepository;
    @Mock private TrainingBranchRepository trainingBranchRepository;
    @Mock private UserLookupService userLookupService;
    @Mock private DomainSecurityService domainSecurityService;

    private OrganisationSecurityService service;

    @BeforeEach
    void setUp() {
        // A real request context so the memoisation under test is actually exercised.
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        service = new OrganisationSecurityService(
                studentGroupRepository, trainingBranchRepository, userLookupService,
                domainSecurityService, new RequestScopedCache());

        authenticateAsJwtUser();
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(CALLER_UUID);
        when(domainSecurityService.isPlatformAdmin()).thenReturn(false);
        when(userLookupService.userBelongsToOrganization(any(), any())).thenReturn(false);
        when(userLookupService.userBelongsToOrganizationWithDomain(any(), any(), any())).thenReturn(false);
        when(studentGroupRepository.findByUuid(GROUP_UUID)).thenReturn(Optional.of(group(ORG_UUID)));
        when(trainingBranchRepository.findByUuidAndDeletedFalse(BRANCH_UUID))
                .thenReturn(Optional.of(branch(ORG_UUID)));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    // ===== ORGANISATION SCOPE =====

    @Test
    void aPlatformAdminMayReadAndManageAnyOrganisation() {
        when(domainSecurityService.isPlatformAdmin()).thenReturn(true);

        assertThat(service.canReadOrganisation(ORG_UUID)).isTrue();
        assertThat(service.canManageOrganisation(ORG_UUID)).isTrue();
        assertThat(service.canManageOrganisation(OTHER_ORG_UUID)).isTrue();
    }

    @Test
    void anOrgScopedAdminOfThisOrganisationMayManageIt() {
        belongsWithDomain(ORG_UUID, UserDomain.admin);

        assertThat(service.canManageOrganisation(ORG_UUID)).isTrue();
    }

    @Test
    void anOrganisationUserOfThisOrganisationMayManageIt() {
        belongsWithDomain(ORG_UUID, UserDomain.organisation_user);

        assertThat(service.canManageOrganisation(ORG_UUID)).isTrue();
    }

    @Test
    void aPlainMemberMayReadButNotManage() {
        when(userLookupService.userBelongsToOrganization(CALLER_UUID, ORG_UUID)).thenReturn(true);

        assertThat(service.canReadOrganisation(ORG_UUID)).isTrue();
        assertThat(service.canManageOrganisation(ORG_UUID)).isFalse();
    }

    @Test
    void anAdminOfADifferentOrganisationGetsNothingHere() {
        // The actual hole: authority in one organisation must not reach another.
        belongsWithDomain(OTHER_ORG_UUID, UserDomain.admin);
        when(userLookupService.userBelongsToOrganization(CALLER_UUID, OTHER_ORG_UUID)).thenReturn(true);

        assertThat(service.canManageOrganisation(ORG_UUID)).isFalse();
        assertThat(service.canReadOrganisation(ORG_UUID)).isFalse();
        assertThat(service.canManageOrganisation(OTHER_ORG_UUID)).isTrue();
    }

    @Test
    void aCallerWithNoOrganisationIsRefused() {
        assertThat(service.canReadOrganisation(ORG_UUID)).isFalse();
        assertThat(service.canManageOrganisation(ORG_UUID)).isFalse();
    }

    @Test
    void anUnauthenticatedCallerIsRefused() {
        SecurityContextHolder.clearContext();
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(null);

        assertThat(service.canReadOrganisation(ORG_UUID)).isFalse();
        assertThat(service.canManageOrganisation(ORG_UUID)).isFalse();
        assertThat(service.canReadGroup(GROUP_UUID)).isFalse();
        assertThat(service.canManageBranch(BRANCH_UUID)).isFalse();
    }

    @Test
    void aNullOrganisationIsRefusedWithoutConsultingAnyone() {
        when(domainSecurityService.isPlatformAdmin()).thenReturn(true);

        assertThat(service.canReadOrganisation(null)).isFalse();
        assertThat(service.canManageOrganisation(null)).isFalse();
    }

    @Test
    void aDomainLookupFailureDeniesRatherThanGrants() {
        when(userLookupService.userBelongsToOrganizationWithDomain(any(), any(), any()))
                .thenThrow(new IllegalStateException("lookup exploded"));

        assertThat(service.canManageOrganisation(ORG_UUID)).isFalse();
    }

    // ===== GROUP SCOPE =====

    @Test
    void aManagerOfTheOwningOrganisationMayManageItsGroups() {
        belongsWithDomain(ORG_UUID, UserDomain.organisation_user);

        assertThat(service.canManageGroup(GROUP_UUID)).isTrue();
    }

    @Test
    void aMemberOfTheOwningOrganisationMayReadItsGroupsButNotChangeThem() {
        when(userLookupService.userBelongsToOrganization(CALLER_UUID, ORG_UUID)).thenReturn(true);

        assertThat(service.canReadGroup(GROUP_UUID)).isTrue();
        assertThat(service.canManageGroup(GROUP_UUID)).isFalse();
    }

    @Test
    void anAdminOfADifferentOrganisationCannotTouchThisOrganisationsGroup() {
        belongsWithDomain(OTHER_ORG_UUID, UserDomain.admin);
        when(userLookupService.userBelongsToOrganization(CALLER_UUID, OTHER_ORG_UUID)).thenReturn(true);

        assertThat(service.canManageGroup(GROUP_UUID)).isFalse();
        assertThat(service.canReadGroup(GROUP_UUID)).isFalse();
    }

    @Test
    void aGroupThatDoesNotExistIsRefused() {
        when(domainSecurityService.isPlatformAdmin()).thenReturn(true);
        when(studentGroupRepository.findByUuid(GROUP_UUID)).thenReturn(Optional.empty());

        // Even a platform admin gets false: an unresolvable group has no organisation to authorise against.
        assertThat(service.canReadGroup(GROUP_UUID)).isFalse();
        assertThat(service.canManageGroup(GROUP_UUID)).isFalse();
    }

    @Test
    void aNullGroupIsRefused() {
        when(domainSecurityService.isPlatformAdmin()).thenReturn(true);

        assertThat(service.canReadGroup(null)).isFalse();
        assertThat(service.canManageGroup(null)).isFalse();
    }

    @Test
    void aGroupLookupFailureDeniesRatherThanGrants() {
        when(domainSecurityService.isPlatformAdmin()).thenReturn(true);
        when(studentGroupRepository.findByUuid(GROUP_UUID)).thenThrow(new IllegalStateException("boom"));

        assertThat(service.canManageGroup(GROUP_UUID)).isFalse();
    }

    @Test
    void theOwningOrganisationOfAGroupIsResolvedOncePerRequest() {
        // The group-scoped routes carry no organisation in the path, and adding members resolves it
        // twice over a single request. Without memoisation each guarded call is another round-trip.
        belongsWithDomain(ORG_UUID, UserDomain.organisation_user);
        when(userLookupService.userBelongsToOrganization(CALLER_UUID, ORG_UUID)).thenReturn(true);

        assertThat(service.canReadGroup(GROUP_UUID)).isTrue();
        assertThat(service.canManageGroup(GROUP_UUID)).isTrue();
        assertThat(service.canManageGroup(GROUP_UUID)).isTrue();

        verify(studentGroupRepository, times(1)).findByUuid(GROUP_UUID);
    }

    // ===== BRANCH SCOPE =====

    @Test
    void aManagerOfTheOwningOrganisationMayManageItsBranches() {
        belongsWithDomain(ORG_UUID, UserDomain.admin);

        assertThat(service.canManageBranch(BRANCH_UUID)).isTrue();
    }

    @Test
    void aMemberOfTheOwningOrganisationMayReadItsBranchesButNotChangeThem() {
        when(userLookupService.userBelongsToOrganization(CALLER_UUID, ORG_UUID)).thenReturn(true);

        assertThat(service.canReadBranch(BRANCH_UUID)).isTrue();
        assertThat(service.canManageBranch(BRANCH_UUID)).isFalse();
    }

    @Test
    void anAdminOfADifferentOrganisationCannotTouchThisOrganisationsBranch() {
        belongsWithDomain(OTHER_ORG_UUID, UserDomain.admin);
        when(userLookupService.userBelongsToOrganization(CALLER_UUID, OTHER_ORG_UUID)).thenReturn(true);

        assertThat(service.canManageBranch(BRANCH_UUID)).isFalse();
        assertThat(service.canReadBranch(BRANCH_UUID)).isFalse();
    }

    @Test
    void aBranchThatDoesNotExistOrIsDeletedIsRefused() {
        when(domainSecurityService.isPlatformAdmin()).thenReturn(true);
        when(trainingBranchRepository.findByUuidAndDeletedFalse(BRANCH_UUID)).thenReturn(Optional.empty());

        assertThat(service.canReadBranch(BRANCH_UUID)).isFalse();
        assertThat(service.canManageBranch(BRANCH_UUID)).isFalse();
    }

    @Test
    void aNullBranchIsRefused() {
        when(domainSecurityService.isPlatformAdmin()).thenReturn(true);

        assertThat(service.canReadBranch(null)).isFalse();
        assertThat(service.canManageBranch(null)).isFalse();
    }

    @Test
    void theOwningOrganisationOfABranchIsResolvedOncePerRequest() {
        belongsWithDomain(ORG_UUID, UserDomain.admin);
        when(userLookupService.userBelongsToOrganization(CALLER_UUID, ORG_UUID)).thenReturn(true);

        assertThat(service.canReadBranch(BRANCH_UUID)).isTrue();
        assertThat(service.canManageBranch(BRANCH_UUID)).isTrue();

        verify(trainingBranchRepository, times(1)).findByUuidAndDeletedFalse(BRANCH_UUID);
    }

    private void belongsWithDomain(UUID organisationUuid, UserDomain domain) {
        when(userLookupService.userBelongsToOrganizationWithDomain(CALLER_UUID, organisationUuid, domain))
                .thenReturn(true);
    }

    private static StudentGroup group(UUID organisationUuid) {
        StudentGroup group = new StudentGroup();
        group.setOrganisationUuid(organisationUuid);
        return group;
    }

    private static TrainingBranch branch(UUID organisationUuid) {
        TrainingBranch branch = new TrainingBranch();
        branch.setOrganisationUuid(organisationUuid);
        return branch;
    }

    private void authenticateAsJwtUser() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", KEYCLOAK_ID)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(jwt, null, "ROLE_USER");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
