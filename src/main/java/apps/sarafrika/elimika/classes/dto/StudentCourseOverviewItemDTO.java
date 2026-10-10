package apps.sarafrika.elimika.classes.dto;

import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "StudentCourseOverviewItem",
        description = "One class the learner is enrolled in, with its course, instructor, next session, "
                + "course progress and outstanding assessments."
)
public record StudentCourseOverviewItemDTO(

        @Schema(description = "Class definition identifier", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("class_definition_uuid")
        UUID classDefinitionUuid,

        @Schema(description = "Class title")
        @JsonProperty("class_title")
        String classTitle,

        @Schema(description = "Class thumbnail URL")
        @JsonProperty("class_thumbnail_url")
        String classThumbnailUrl,

        @Schema(description = "Organisation running the class, if any")
        @JsonProperty("organisation_uuid")
        UUID organisationUuid,

        @Schema(description = "Most recent scheduled-instance enrolment for this class")
        @JsonProperty("latest_enrollment_uuid")
        UUID latestEnrollmentUuid,

        @Schema(description = "Status of the most recent scheduled-instance enrolment")
        @JsonProperty("latest_enrollment_status")
        EnrollmentStatus latestEnrollmentStatus,

        @Schema(description = "Number of scheduled-instance enrolments under this class")
        @JsonProperty("scheduled_instance_count")
        int scheduledInstanceCount,

        @Schema(description = "Course the class teaches")
        @JsonProperty("course_uuid")
        UUID courseUuid,

        @Schema(description = "Course name")
        @JsonProperty("course_name")
        String courseName,

        @Schema(description = "Training programme the class teaches, if any")
        @JsonProperty("program_uuid")
        UUID programUuid,

        @Schema(description = "Instructor of the class (default instructor, else the next session's)")
        @JsonProperty("instructor_uuid")
        UUID instructorUuid,

        @Schema(description = "Instructor display name")
        @JsonProperty("instructor_name")
        String instructorName,

        @Schema(description = "Current or next session; null when nothing is left to attend", nullable = true)
        @JsonProperty("next_session")
        StudentCourseOverviewSessionDTO nextSession,

        @Schema(description = "Learner's course enrolment identifier", nullable = true)
        @JsonProperty("course_enrollment_uuid")
        UUID courseEnrollmentUuid,

        @Schema(description = "Learner's course enrolment status", nullable = true)
        @JsonProperty("course_enrollment_status")
        String courseEnrollmentStatus,

        @Schema(description = "Course progress percentage", nullable = true)
        @JsonProperty("progress_percentage")
        BigDecimal progressPercentage,

        @Schema(description = "Released class assignments the learner has not handed in")
        @JsonProperty("pending_assignment_count")
        int pendingAssignmentCount,

        @Schema(description = "Released class quizzes the learner has not submitted")
        @JsonProperty("pending_quiz_count")
        int pendingQuizCount,

        @Schema(description = "Most recent class enrolment activity", format = "date-time")
        @JsonProperty("latest_activity_date")
        LocalDateTime latestActivityDate
) {
}
