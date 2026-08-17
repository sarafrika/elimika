package apps.sarafrika.elimika.tenancy.spi;

import java.util.UUID;

/**
 * Organisation Affiliation Service Provider Interface
 * <p>
 * Lets other modules attach a user to an organisation as the result of a domain event
 * they own, without exposing tenancy entities, repositories or consent internals.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-08-13
 */
public interface OrganisationAffiliationService {

    /**
     * Affiliates an instructor with an organisation because that organisation hired them
     * through a marketplace class job they applied to.
     * <p>
     * The affiliation is recorded in the {@code instructor} domain, with consent traced to
     * the instructor's own act of applying.
     * <p>
     * A user may hold only one active affiliation per organisation. If the instructor is
     * already affiliated - in any domain - the existing affiliation is left untouched so a
     * hire never silently downgrades or overwrites an existing role.
     *
     * @param userUuid         the instructor's user account
     * @param organisationUuid the hiring organisation
     * @param branchUuid       the training branch to attach them to, or {@code null}
     * @return {@code true} if a new affiliation was created, {@code false} if one already existed
     */
    boolean affiliateHiredInstructor(UUID userUuid, UUID organisationUuid, UUID branchUuid);
}
