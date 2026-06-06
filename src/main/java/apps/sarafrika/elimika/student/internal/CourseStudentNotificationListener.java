package apps.sarafrika.elimika.student.internal;

import apps.sarafrika.elimika.course.spi.AssessmentCompletedNotificationRequestedEvent;
import apps.sarafrika.elimika.course.spi.LearningCertificateIssuedNotificationRequestedEvent;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseStudentNotificationListener {

    private final StudentRepository studentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @ApplicationModuleListener
    void onAssessmentCompleted(AssessmentCompletedNotificationRequestedEvent event) {
        UUID recipientUserUuid = resolveUserUuid(event.studentUuid());
        if (recipientUserUuid == null) {
            return;
        }

        String assessmentType = value(event.assessmentType(), "assessment");
        String assessmentTitle = value(event.assessmentTitle(), "Assessment");

        eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                recipientUserUuid,
                "ASSESSMENT_COMPLETED",
                "POPUP",
                "Assessment completed",
                assessmentTitle + " has been submitted.",
                "/dashboard/courses/" + value(event.courseUuid()) + "/assessments",
                Map.of(
                        "assessment_source_uuid", value(event.assessmentSourceUuid()),
                        "assessment_uuid", value(event.assessmentUuid()),
                        "assessment_title", assessmentTitle,
                        "assessment_type", assessmentType,
                        "enrollment_uuid", value(event.enrollmentUuid())
                ),
                "assessment-completed:" + assessmentType + ":" + event.assessmentSourceUuid()
        ));
    }

    @ApplicationModuleListener
    void onCertificateIssued(LearningCertificateIssuedNotificationRequestedEvent event) {
        UUID recipientUserUuid = resolveUserUuid(event.studentUuid());
        if (recipientUserUuid == null) {
            return;
        }

        String learningTitle = value(event.learningTitle(), "your learning");

        eventPublisher.publishEvent(NotificationRequestedEvent.inApp(
                recipientUserUuid,
                "LEARNING_CERTIFICATE_ISSUED",
                "POPUP",
                "Certificate issued",
                "Your certificate for " + learningTitle + " is ready.",
                "/dashboard/profile/certificates",
                Map.of(
                        "certificate_uuid", value(event.certificateUuid()),
                        "certificate_number", value(event.certificateNumber()),
                        "course_uuid", value(event.courseUuid()),
                        "program_uuid", value(event.programUuid()),
                        "learning_title", learningTitle
                ),
                "certificate-issued:" + event.certificateUuid()
        ));
    }

    private UUID resolveUserUuid(UUID studentUuid) {
        if (studentUuid == null) {
            return null;
        }
        return studentRepository.findByUuid(studentUuid)
                .map(student -> student.getUserUuid())
                .orElse(null);
    }

    private String value(UUID value) {
        return value == null ? "" : value.toString();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
