package apps.sarafrika.elimika.shared.security;

import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.spi.contact.ContactRelationshipSource;
import apps.sarafrika.elimika.shared.spi.contact.ContactRelationshipSource.Party;
import apps.sarafrika.elimika.student.spi.StudentLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Who may see a user's email, phone number and date of birth.
 * <p>
 * The user record is read by every roster, waiting list and enrolment table in the product, so the
 * route that serves it cannot be closed to anyone but the account holder. What it can do is answer
 * with the directory projection unless the caller has a reason to hold the person's contact
 * details. This service is that reason, expressed once so that every caller — the record route
 * today, whatever needs it next — asks the same question.
 * <p>
 * Four grounds, tested cheapest first:
 * <ol>
 *   <li><strong>Self.</strong> Free: the caller's own UUID is already memoised for the request.</li>
 *   <li><strong>Platform administrator.</strong> One lookup, and support cannot work without it.</li>
 *   <li><strong>Manager of an organisation the subject belongs to.</strong> The organisation's own
 *       roster routes already carry these fields for its members, so an org manager learns nothing
 *       here they could not read there.</li>
 *   <li><strong>A working relationship</strong> contributed by whichever module owns the link —
 *       see {@link ContactRelationshipSource}. This is the branch that keeps an instructor able to
 *       phone a learner on their own waiting list.</li>
 * </ol>
 * Deliberately <em>not</em> a ground: holding the {@code instructor}, {@code course_creator} or
 * {@code admin} domain. A domain a user can hold globally says nothing about the person being read,
 * so it would amount to handing the platform's contact list to anyone who signs up as an instructor.
 * Every ground above is scoped to the subject.
 * <p>
 * The whole answer is memoised per request and per subject, because the relationship sources cost a
 * query each and a single request can ask more than once. Fails closed throughout.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-09-04
 */
@Service("userContactSecurityService")
@RequiredArgsConstructor
@Slf4j
public class UserContactSecurityService {

    private static final String CACHE_CONTACT_VISIBLE_PREFIX = "security.contactVisible.";
    private static final String CACHE_CALLER_PARTY = "security.contactParty.caller";
    private static final String CACHE_PARTY_PREFIX = "security.contactParty.";

    private final DomainSecurityService domainSecurityService;
    private final StudentLookupService studentLookupService;
    private final InstructorLookupService instructorLookupService;
    private final RequestScopedCache requestScopedCache;
    /**
     * Injected lazily as a provider rather than as a {@code List} so that a context which happens to
     * load this service without any relationship module still starts — it simply grants nothing on
     * relationship grounds, which is the safe direction.
     */
    private final ObjectProvider<ContactRelationshipSource> relationshipSources;

    /**
     * True when the caller may be shown this user's contact details.
     *
     * @param targetUserUuid the account being read
     */
    public boolean canReadContactDetails(UUID targetUserUuid) {
        if (targetUserUuid == null) {
            return false;
        }
        return requestScopedCache.get(CACHE_CONTACT_VISIBLE_PREFIX + targetUserUuid,
                () -> resolve(targetUserUuid));
    }

    private boolean resolve(UUID targetUserUuid) {
        try {
            UUID callerUuid = domainSecurityService.getCurrentUserUuid();
            if (callerUuid == null) {
                return false;
            }
            if (callerUuid.equals(targetUserUuid)) {
                return true;
            }
            if (domainSecurityService.isPlatformAdmin()) {
                return true;
            }
            if (domainSecurityService.managesOrganisationOf(targetUserUuid)) {
                return true;
            }
            return hasWorkingRelationship(callerUuid, targetUserUuid);
        } catch (Exception e) {
            log.error("Error deciding contact visibility for user {}", targetUserUuid, e);
            return false;
        }
    }

    /**
     * Asks each module that owns a person-to-person link, stopping at the first that says yes.
     * <p>
     * Both parties are resolved to their student and instructor profiles once, here, so that a
     * source implementation never repeats those lookups and can decide from the identifiers alone.
     * A subject with no student and no instructor profile cannot be on the other end of any link
     * this expresses, so the sources are not consulted at all.
     */
    private boolean hasWorkingRelationship(UUID callerUuid, UUID targetUserUuid) {
        Party subject = party(targetUserUuid);
        if (subject.hasNoProfile()) {
            return false;
        }
        Party viewer = requestScopedCache.get(CACHE_CALLER_PARTY, () -> party(callerUuid));
        return relationshipSources.stream()
                .anyMatch(source -> source.viewerMayContactSubject(viewer, subject));
    }

    private Party party(UUID userUuid) {
        return requestScopedCache.get(CACHE_PARTY_PREFIX + userUuid, () -> new Party(
                userUuid,
                studentLookupService.findStudentUuidByUserUuid(userUuid).orElse(null),
                instructorLookupService.findInstructorUuidByUserUuid(userUuid).orElse(null)));
    }
}
