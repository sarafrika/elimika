package apps.sarafrika.elimika.shared.security;

import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.student.spi.StudentLookupService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Unified security service for domain-based authorization checks.
 * Replaces role-based authorization with domain-based checks.
 * <p>
 * Refactored to use Spring Modulith SPIs instead of direct repository access.
 */
@Service("domainSecurityService")
@RequiredArgsConstructor
@Slf4j
public class DomainSecurityService {

    private static final String CACHE_USER_UUID = "security.userUuid";
    private static final String CACHE_STUDENT_UUID = "security.studentUuid";
    private static final String CACHE_DOMAINS = "security.domains";
    private static final String CACHE_ADMINISTERS_USER_PREFIX = "security.administersUser.";

    private final UserLookupService userLookupService;
    private final StudentLookupService studentLookupService;
    private final InstructorLookupService instructorLookupService;
    private final RequestScopedCache requestScopedCache;

    /**
     * Checks if the currently authenticated user is a student.
     */
    public boolean isStudent() {
        return hasUserDomain(UserDomain.student);
    }

    /**
     * Checks if the currently authenticated user is an instructor.
     */
    public boolean isInstructor() {
        return hasUserDomain(UserDomain.instructor);
    }

    /**
     * Checks if the currently authenticated user is an organization admin.
     */
    public boolean isOrganizationAdmin() {
        return hasUserDomain(UserDomain.admin);
    }

    /**
     * Checks if the current user is a PLATFORM admin, i.e. holds the {@code admin}
     * domain at the global level (not merely as an org-scoped role). Use this to
     * protect platform-infrastructure endpoints (e.g. currency and platform-fee
     * configuration) from org-scoped admins, who legitimately manage their own
     * organisation but must not administer platform-wide settings.
     */
    public boolean isPlatformAdmin() {
        try {
            UUID currentUserUuid = getCurrentUserUuid();
            return currentUserUuid != null && userLookupService.userHasGlobalDomain(currentUserUuid, UserDomain.admin);
        } catch (Exception e) {
            log.error("Error checking platform admin status", e);
            return false;
        }
    }

    /**
     * True when the caller holds the {@code admin} domain <em>in an organisation the target user
     * belongs to</em>.
     * <p>
     * This is the org-scoped counterpart to {@link #isOrganizationAdmin()}. That method answers
     * "does this caller administer something, anywhere" — it reads the union of the caller's global
     * and organisation-scoped domains and never looks at who is being acted on, so an administrator
     * of organisation A satisfies it while operating on a member of organisation B. Wherever the
     * subject of the request is another user, this is the predicate that was meant.
     * <p>
     * Lives here rather than beside the tenancy predicates because the modules that need it — the
     * wallet in particular — are only permitted to depend on {@code shared}. Fails closed, and is
     * memoised per request and per target because the answer costs a query per organisation the
     * target belongs to.
     *
     * @param targetUserUuid the user being acted on
     */
    public boolean administersOrganisationOf(UUID targetUserUuid) {
        if (targetUserUuid == null) {
            return false;
        }
        return requestScopedCache.get(CACHE_ADMINISTERS_USER_PREFIX + targetUserUuid, () -> {
            try {
                UUID callerUuid = getCurrentUserUuid();
                if (callerUuid == null) {
                    return false;
                }
                return userLookupService.getUserOrganizations(targetUserUuid).stream()
                        .anyMatch(organisationUuid -> userLookupService.userBelongsToOrganizationWithDomain(
                                callerUuid, organisationUuid, UserDomain.admin));
            } catch (Exception e) {
                log.error("Error checking administrative reach over user {}", targetUserUuid, e);
                return false;
            }
        });
    }

    /**
     * Checks if the currently authenticated user is a course creator.
     */
    public boolean isCourseCreator() {
        return hasUserDomain(UserDomain.course_creator);
    }

    /**
     * Checks if the currently authenticated user is a guardian/parent.
     */
    public boolean isGuardian() {
        return hasUserDomain(UserDomain.parent);
    }

    /**
     * Checks if the current user has any of the specified domains.
     * <p>
     * Answered from the caller's cached domain set, so testing N domains costs one load rather
     * than N lookups.
     */
    public boolean hasAnyDomain(UserDomain... domains) {
        if (domains == null || domains.length == 0) {
            return false;
        }
        Set<UserDomain> effective = currentUserDomains();
        for (UserDomain domain : domains) {
            if (effective.contains(domain)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The current caller's effective domains, loaded once per request.
     * <p>
     * Every {@code @PreAuthorize} on a request funnels through here, so this is the hottest path in
     * the authorization layer. Resolving it once and answering from a set keeps a guarded endpoint
     * at a constant cost no matter how many domains its expression tests.
     */
    private Set<UserDomain> currentUserDomains() {
        return requestScopedCache.get(CACHE_DOMAINS, () -> {
            try {
                UUID currentUserUuid = getCurrentUserUuid();
                if (currentUserUuid == null) {
                    log.debug("No authenticated user found");
                    return Set.<UserDomain>of();
                }
                return userLookupService.getEffectiveUserDomains(currentUserUuid);
            } catch (Exception e) {
                log.error("Error loading user domains", e);
                return Set.<UserDomain>of();
            }
        });
    }

    /**
     * Checks if the current user is a student OR an instructor OR an org admin.
     * This is a common permission pattern in the system.
     */
    public boolean isStudentOrInstructorOrAdmin() {
        return hasAnyDomain(UserDomain.student, UserDomain.instructor, UserDomain.admin);
    }

    /**
     * Checks if the current user is an instructor OR an org admin.
     * This is a common permission pattern in the system.
     */
    public boolean isInstructorOrAdmin() {
        return hasAnyDomain(UserDomain.instructor, UserDomain.admin);
    }

    /**
     * Checks if the currently authenticated user belongs to a specific instructor.
     */
    public boolean isInstructorWithUuid(UUID instructorUuid) {
        try {
            UUID currentUserUuid = getCurrentUserUuid();
            if (currentUserUuid == null) {
                return false;
            }

            UUID instructorUserUuid = instructorLookupService.findInstructorUuidByUserUuid(currentUserUuid).orElse(null);
            if (instructorUserUuid == null) {
                return false;
            }

            return instructorUserUuid.equals(instructorUuid);
        } catch (Exception e) {
            log.error("Error checking instructor identity for instructorUuid: {}", instructorUuid, e);
            return false;
        }
    }

    /**
     * Checks if the currently authenticated user belongs to a specific student.
     */
    public boolean isStudentWithUuid(UUID studentUuid) {
        try {
            UUID currentUserUuid = getCurrentUserUuid();
            if (currentUserUuid == null) {
                return false;
            }

            UUID studentUserUuid = studentLookupService.findStudentUuidByUserUuid(currentUserUuid).orElse(null);
            if (studentUserUuid == null) {
                return false;
            }

            return studentUserUuid.equals(studentUuid);
        } catch (Exception e) {
            log.error("Error checking student identity for studentUuid: {}", studentUuid, e);
            return false;
        }
    }

    /**
     * Prevents admins who hold additional user domains from self-approving their own profiles.
     *
     * @param profileOwnerUuid UUID of the profile owner being approved
     * @param profileLabel     Human friendly profile label for the error message
     */
    public void enforceNotSelfApprovingProfile(UUID profileOwnerUuid, String profileLabel) {
        UUID currentUserUuid = getCurrentUserUuid();
        if (currentUserUuid == null || profileOwnerUuid == null) {
            return;
        }

        if (!currentUserUuid.equals(profileOwnerUuid)) {
            return;
        }

        if (isAdminWithAdditionalDomains(currentUserUuid)) {
            throw new AccessDeniedException(
                    String.format("Admins with multiple domains cannot approve their own %s profile.", profileLabel)
            );
        }
    }

    /**
     * Prevents admins who hold additional user domains from approving organisations they belong to.
     *
     * @param organisationUuid target organisation identifier
     */
    public void enforceNotSelfApprovingOrganisation(UUID organisationUuid) {
        UUID currentUserUuid = getCurrentUserUuid();
        if (currentUserUuid == null || organisationUuid == null) {
            return;
        }

        if (!isAdminWithAdditionalDomains(currentUserUuid)) {
            return;
        }

        if (userLookupService.userBelongsToOrganization(currentUserUuid, organisationUuid)) {
            throw new AccessDeniedException(
                    "Admins with multiple domains cannot approve organisations they belong to."
            );
        }
    }

    /**
     * Gets the student UUID of the current authenticated user, or null when the caller
     * is not authenticated or has no student profile. Use this to scope learner-owned
     * records to the caller rather than trusting a client-supplied student identifier.
     */
    public UUID getCurrentStudentUuid() {
        return requestScopedCache.get(CACHE_STUDENT_UUID, () -> {
            try {
                UUID currentUserUuid = getCurrentUserUuid();
                if (currentUserUuid == null) {
                    return null;
                }
                return studentLookupService.findStudentUuidByUserUuid(currentUserUuid).orElse(null);
            } catch (Exception e) {
                log.error("Error resolving current student UUID", e);
                return null;
            }
        });
    }

    /**
     * Gets the UUID of the current authenticated user.
     * Returns null if no user is authenticated.
     */
    public UUID getCurrentUserUuid() {
        return requestScopedCache.get(CACHE_USER_UUID, () -> {
            try {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !authentication.isAuthenticated()) {
                    return null;
                }

                String keycloakId = getKeycloakId(authentication);
                if (keycloakId == null) {
                    return null;
                }

                return userLookupService.findUserUuidByKeycloakId(keycloakId).orElse(null);
            } catch (Exception e) {
                log.error("Error getting current user UUID", e);
                return null;
            }
        });
    }

    private boolean isAdminWithAdditionalDomains(UUID userUuid) {
        List<UserDomain> domains = userLookupService.getUserDomains(userUuid);
        boolean hasAdminDomain = domains.stream().anyMatch(UserDomain.admin::equals);
        boolean hasOtherDomains = domains.stream().anyMatch(domain -> domain != UserDomain.admin);
        return hasAdminDomain && hasOtherDomains;
    }

    /**
     * Checks if the current user has a specific user domain.
     */
    private boolean hasUserDomain(UserDomain domain) {
        return currentUserDomains().contains(domain);
    }

    /**
     * Extracts the Keycloak ID from the JWT token.
     */
    private String getKeycloakId(Authentication authentication) {
        try {
            if (authentication.getPrincipal() instanceof Jwt jwt) {
                return jwt.getClaimAsString("sub");
            }
        } catch (Exception e) {
            log.error("Error extracting Keycloak ID from JWT", e);
        }
        return null;
    }
}
