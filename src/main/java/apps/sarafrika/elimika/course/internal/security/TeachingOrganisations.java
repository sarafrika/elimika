package apps.sarafrika.elimika.course.internal.security;

import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * The organisations a user belongs to <em>as teaching staff</em>.
 * <p>
 * Extracted so the rule below has exactly one definition. Two separate questions need it — which
 * courses a caller may mark, and which footing they view a course's content on — and if each
 * carried its own copy, widening one would silently leave the other behind.
 */
@Component
@RequiredArgsConstructor
public class TeachingOrganisations {

    /**
     * Organisation-scoped roles that make a member part of an organisation's <em>teaching</em> side.
     * <p>
     * Membership alone is not one of them. An organisation's roster mixes its staff with the
     * learners it enrolled, both carried by rows in the same table, and only the org-scoped domain
     * tells them apart — so a training approval granted to an organisation must be read as granting
     * its staff, never everyone it has ever invited.
     */
    private static final List<UserDomain> ORGANISATION_TEACHING_DOMAINS = List.of(
            UserDomain.organisation_user, UserDomain.admin,
            UserDomain.instructor, UserDomain.course_creator);

    private final UserLookupService userLookupService;

    /**
     * @param userUuid the user to resolve, or null
     * @return the organisations they staff, empty when there are none or the user is unknown
     */
    public List<UUID> of(UUID userUuid) {
        if (userUuid == null) {
            return List.of();
        }
        return userLookupService.getUserOrganizations(userUuid).stream()
                .filter(organisationUuid -> ORGANISATION_TEACHING_DOMAINS.stream()
                        .anyMatch(domain -> userLookupService.userBelongsToOrganizationWithDomain(
                                userUuid, organisationUuid, domain)))
                .toList();
    }
}
