package apps.sarafrika.elimika.shared.spi.instructor;

import java.util.UUID;

/**
 * Answers whether a caller is currently reviewing an application the instructor themselves made.
 * <p>
 * An instructor's credential documents are identity-grade material, so reading them takes more than
 * a role: it takes a relationship the instructor created by applying for something. Each module that
 * accepts instructor applications — course and programme training, the class marketplace —
 * contributes one implementation, and the instructor module grants a reviewer exactly while one of
 * them says the application exists. This keeps the grant subject-scoped: holding
 * {@code course_creator} or an organisation role says nothing on its own, because the applicant has
 * to have applied to <em>your</em> course or <em>your</em> organisation's job.
 * <p>
 * Implementations must fail closed and must never throw.
 */
public interface InstructorCredentialReviewerLookup {

    /**
     * @param instructorUuid   the instructor whose credentials are being read
     * @param reviewerUserUuid the base user UUID of the caller
     * @return true when the caller is on the deciding side of an application this instructor made
     */
    boolean isReviewingApplicationFrom(UUID instructorUuid, UUID reviewerUserUuid);
}
