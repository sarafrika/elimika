package apps.sarafrika.elimika.shared.security;

import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Answers the one question every class-scoped guard needs: does this caller run <em>this</em> class?
 * <p>
 * A class has exactly two owners in the schema — {@code organisation_uuid} when an organisation runs
 * it, and {@code default_instructor_uuid} for the trainer delivering it — and every method here is
 * asked against those columns on that class. Holding {@code instructor} or {@code admin} somewhere
 * else on the platform says nothing about a class you have no relationship with, which is precisely
 * the gap this closes: a global domain is self-grantable in effect, so it can never stand in for
 * ownership of a particular party's data.
 * <p>
 * The organisation branch reads an org-scoped {@code organisation_user} or {@code admin} mapping for
 * that organisation specifically, never the global "administers something, anywhere" reading. It is
 * the same test the class service already applies before disclosing {@code instructor_pay}, and for
 * the same reason: that pay is an obligation the organisation owes, so whoever can create or re-home
 * a class can spend that organisation's money.
 * <p>
 * It lives in {@code shared} rather than beside the class definitions because both the classes
 * module (its own controllers) and the timetabling module (enrolment rosters keyed on a class) have
 * to ask it, and both are permitted to depend on {@code shared}. The class's owning columns arrive
 * through {@link ClassDefinitionLookupService}, the cross-module contract the classes module already
 * publishes, so no module boundary is crossed to answer the question.
 * <p>
 * Every method fails closed: a missing class, an unresolvable caller or a lookup failure denies.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-09-04
 */
@Service("classAccessSecurityService")
@RequiredArgsConstructor
@Slf4j
public class ClassAccessSecurityService {

    private static final String CACHE_MANAGES_CLASS_PREFIX = "security.managesClass.";
    private static final String CACHE_CLASS_ORG_PREFIX = "security.classOrg.";
    private static final String CACHE_MANAGES_ORG_PREFIX = "security.managesOrganisation.";

    private final ClassDefinitionLookupService classDefinitionLookupService;
    private final UserLookupService userLookupService;
    private final DomainSecurityService domainSecurityService;
    private final RequestScopedCache requestScopedCache;

    /**
     * True when the caller may act on an existing class — change its details, its media, its session
     * templates, its assessment schedule or its active flag, and read the roster sitting in it.
     * <p>
     * Three parties qualify, and only in relation to this class: the platform admin, the instructor
     * the class is assigned to, and a manager of the organisation that owns it. A class with no
     * organisation is the instructor's own, and then the instructor branch is the only way through.
     * <p>
     * Deliberately <em>not</em> extended to whoever is named on a scheduled instance of the class:
     * any instructor may schedule an instance through the timetabling API, so that would be a
     * self-grantable route back into someone else's class.
     * <p>
     * Memoised per request and per class because a single request may consult it many times — once
     * per row of a roster page — and each answer costs a class read plus a membership query.
     *
     * @param classDefinitionUuid the class being acted on
     */
    public boolean canManageClass(UUID classDefinitionUuid) {
        if (classDefinitionUuid == null) {
            return false;
        }
        return requestScopedCache.get(CACHE_MANAGES_CLASS_PREFIX + classDefinitionUuid, () -> {
            try {
                // Settled before the class is read so that a platform admin acting on a class that
                // no longer exists still gets the handler's 404 rather than a misleading 403.
                if (domainSecurityService.isPlatformAdmin()) {
                    return true;
                }
                UUID defaultInstructorUuid = classDefinitionLookupService
                        .findDefaultInstructorUuid(classDefinitionUuid)
                        .orElse(null);
                if (defaultInstructorUuid != null
                        && domainSecurityService.isInstructorWithUuid(defaultInstructorUuid)) {
                    return true;
                }
                return managesOrganisation(organisationOfClass(classDefinitionUuid));
            } catch (Exception e) {
                log.error("Error checking management rights over class {}", classDefinitionUuid, e);
                return false;
            }
        });
    }

    /**
     * Management rights over the class, plus rights over the organisation the update would hand it
     * to.
     * <p>
     * The update payload carries {@code organisation_uuid} and the factory applies it, so an
     * instructor could otherwise re-home their own class into any organisation and make that
     * organisation the debtor for the instructor pay on it. A null organisation leaves the class
     * where it is — the factory ignores it — and re-stating the organisation a class already has is
     * not a move either, so neither needs anything extra.
     *
     * @param classDefinitionUuid       the class being updated
     * @param requestedOrganisationUuid the organisation named in the payload, may be null
     */
    public boolean canUpdateClass(UUID classDefinitionUuid, UUID requestedOrganisationUuid) {
        if (!canManageClass(classDefinitionUuid)) {
            return false;
        }
        if (requestedOrganisationUuid == null
                || requestedOrganisationUuid.equals(organisationOfClass(classDefinitionUuid))) {
            return true;
        }
        return managesOrganisation(requestedOrganisationUuid);
    }

    /**
     * True when the caller may create a class on these terms.
     * <p>
     * An organisation-owned class needs a manager of that organisation; a standalone class needs the
     * instructor it is being assigned to, so a trainer can still set up their own class. Platform
     * admins pass either way. An instructor cannot name someone else as the trainer of a standalone
     * class, and cannot name an organisation they do not manage.
     *
     * @param organisationUuid      the organisation the class would belong to, null when standalone
     * @param defaultInstructorUuid the instructor the class would be assigned to
     */
    public boolean canCreateClass(UUID organisationUuid, UUID defaultInstructorUuid) {
        try {
            if (domainSecurityService.isPlatformAdmin()) {
                return true;
            }
            if (organisationUuid != null) {
                return managesOrganisation(organisationUuid);
            }
            return defaultInstructorUuid != null
                    && domainSecurityService.isInstructorWithUuid(defaultInstructorUuid);
        } catch (Exception e) {
            log.error("Error checking class creation rights", e);
            return false;
        }
    }

    /**
     * The imperative form of {@link #canCreateClass(UUID, UUID)}, for the multipart create routes
     * where the payload is assembled from parts inside the handler and so cannot be reached from a
     * route-level expression.
     */
    public void ensureMayCreateClass(UUID organisationUuid, UUID defaultInstructorUuid) {
        if (!canCreateClass(organisationUuid, defaultInstructorUuid)) {
            throw new AccessDeniedException("Not permitted to create a class on these terms.");
        }
    }

    /**
     * True when the caller manages this organisation specifically: a platform admin, or the holder
     * of an org-scoped {@code organisation_user} or {@code admin} mapping for it.
     * <p>
     * Mere membership is not enough. An organisation's learners and its contracted trainers belong
     * to it too, and organisation-wide figures — who is enrolled, how each of them is performing —
     * are the institution's, not theirs.
     *
     * @param organisationUuid the organisation whose books are being opened
     */
    public boolean managesOrganisation(UUID organisationUuid) {
        if (organisationUuid == null) {
            return false;
        }
        return requestScopedCache.get(CACHE_MANAGES_ORG_PREFIX + organisationUuid, () -> {
            try {
                if (domainSecurityService.isPlatformAdmin()) {
                    return true;
                }
                UUID callerUuid = domainSecurityService.getCurrentUserUuid();
                if (callerUuid == null) {
                    log.debug("No authenticated user; refusing management of organisation {}", organisationUuid);
                    return false;
                }
                return userLookupService.userBelongsToOrganizationWithDomain(
                                callerUuid, organisationUuid, UserDomain.organisation_user)
                        || userLookupService.userBelongsToOrganizationWithDomain(
                                callerUuid, organisationUuid, UserDomain.admin);
            } catch (Exception e) {
                log.error("Error checking management rights for organisation {}", organisationUuid, e);
                return false;
            }
        });
    }

    /**
     * The organisation a class currently belongs to, memoised for the request. Null both when the
     * class is standalone and when it cannot be read, which is safe: the caller then has to earn
     * rights over the organisation they named.
     */
    private UUID organisationOfClass(UUID classDefinitionUuid) {
        if (classDefinitionUuid == null) {
            return null;
        }
        return requestScopedCache.get(CACHE_CLASS_ORG_PREFIX + classDefinitionUuid, () -> {
            try {
                return classDefinitionLookupService.findOrganisationUuid(classDefinitionUuid).orElse(null);
            } catch (Exception e) {
                log.error("Error resolving the organisation owning class {}", classDefinitionUuid, e);
                return null;
            }
        });
    }
}
