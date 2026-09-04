package apps.sarafrika.elimika.timetabling.internal;

import apps.sarafrika.elimika.shared.spi.contact.ContactRelationshipSource;
import apps.sarafrika.elimika.timetabling.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Timetabling's answer to "may this caller hold this person's contact details": yes when the caller
 * teaches them.
 * <p>
 * The register is the relationship the product leans on hardest. An instructor's students page, the
 * training hub's waiting list and the class console all render an email column, and every one of
 * them is reading a learner the instructor is scheduled to teach. That link lives here, in the
 * enrolment-to-instance join, so this module answers it rather than exporting the table.
 * <p>
 * Strictly directional. Sharing a class does not make two learners visible to each other: the
 * caller has to be the instructor on the instance, and the subject the enrolled student. That is
 * what keeps a classmate from reading the register.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-09-04
 */
@Service
@RequiredArgsConstructor
@Slf4j
class TaughtLearnerContactRelationship implements ContactRelationshipSource {

    private final EnrollmentRepository enrollmentRepository;

    @Override
    public boolean viewerMayContactSubject(Party viewer, Party subject) {
        if (viewer.instructorUuid() == null || subject.studentUuid() == null) {
            return false;
        }
        try {
            return enrollmentRepository.existsForStudentAndInstructor(
                    subject.studentUuid(), viewer.instructorUuid());
        } catch (Exception e) {
            log.error("Error checking whether instructor {} teaches student {}",
                    viewer.instructorUuid(), subject.studentUuid(), e);
            return false;
        }
    }
}
