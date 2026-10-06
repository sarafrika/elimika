package apps.sarafrika.elimika.instructor.internal;

import apps.sarafrika.elimika.instructor.repository.InstructorRepository;
import apps.sarafrika.elimika.shared.event.user.DomainModeratedEvent;
import apps.sarafrika.elimika.shared.utils.enums.DomainApprovalStatus;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Keeps instructors.admin_verified in step when an admin decides the instructor domain directly. */
@Component
@RequiredArgsConstructor
@Slf4j
class InstructorDomainModerationListener {

    private final InstructorRepository instructorRepository;

    @EventListener
    void onDomainModerated(DomainModeratedEvent event) {
        if (!UserDomain.instructor.name().equalsIgnoreCase(event.userDomain())) {
            return;
        }
        boolean verified = event.status() == DomainApprovalStatus.APPROVED;
        instructorRepository.findByUserUuid(event.userUuid()).ifPresentOrElse(instructor -> {
            if (verified != Boolean.TRUE.equals(instructor.getAdminVerified())) {
                instructor.setAdminVerified(verified);
                instructorRepository.save(instructor);
                log.info("Instructor {} admin_verified={} after domain moderation", instructor.getUuid(), verified);
            }
        }, () -> log.warn("No instructor profile for user {} to mirror the domain decision", event.userUuid()));
    }
}
