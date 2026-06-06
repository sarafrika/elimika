package apps.sarafrika.elimika.tenancy.internal;

import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class ProfileCompletionReminderScheduler {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final InstructorLookupService instructorLookupService;
    private final CourseCreatorLookupService courseCreatorLookupService;

    @Scheduled(cron = "0 0 9 * * *")
    void sendProfileCompletionReminders() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<User> users = userRepository.findByCreatedDateBetween(
                now.minusDays(5),
                now.minusDays(3)
        );

        users.stream()
                .filter(this::hasIncompleteProfile)
                .forEach(this::publishReminder);
    }

    private boolean hasIncompleteProfile(User user) {
        Optional<Boolean> instructorComplete = instructorLookupService.getInstructorProfileCompleteByUserUuid(user.getUuid());
        Optional<Boolean> courseCreatorComplete = courseCreatorLookupService.getCourseCreatorProfileCompleteByUserUuid(user.getUuid());

        boolean hasRoleProfile = instructorComplete.isPresent() || courseCreatorComplete.isPresent();
        if (instructorComplete.isPresent() && Boolean.FALSE.equals(instructorComplete.get())) {
            return true;
        }
        if (courseCreatorComplete.isPresent() && Boolean.FALSE.equals(courseCreatorComplete.get())) {
            return true;
        }
        if (hasRoleProfile) {
            return false;
        }

        return isBlank(user.getProfileImageUrl())
                || isBlank(user.getPhoneNumber())
                || user.getDob() == null
                || user.getGender() == null;
    }

    private void publishReminder(User user) {
        eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                user.getUuid(),
                "PROFILE_COMPLETION_REMINDER",
                "POPUP",
                "Complete your profile",
                "Finish your Elimika profile to keep your learning and training access ready.",
                "/dashboard/profile",
                Map.of(
                        "user_uuid", user.getUuid() == null ? "" : user.getUuid().toString(),
                        "created_date", user.getCreatedDate() == null ? "" : user.getCreatedDate().toString()
                ),
                "profile-completion-reminder:" + user.getUuid() + ":" + createdDateKey(user)
        ));
    }

    private String createdDateKey(User user) {
        return user.getCreatedDate() == null ? "unknown" : user.getCreatedDate().toLocalDate().toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
