package apps.sarafrika.elimika.timetabling.dto;

import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "InstructorStudent",
        description = "A student in one of the organisation's classes taught by the instructor; one row per student per class"
)
public record InstructorStudentDTO(

        @Schema(description = "The student", format = "uuid")
        @JsonProperty(value = "student_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID studentUuid,

        @Schema(description = "The student's full name", example = "Amina Otieno")
        @JsonProperty(value = "student_name", access = JsonProperty.Access.READ_ONLY)
        String studentName,

        @Schema(description = "The class the student is enrolled in", format = "uuid")
        @JsonProperty(value = "class_definition_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID classDefinitionUuid,

        @Schema(description = "Title of the class", example = "Grade 5 Piano - Term 2")
        @JsonProperty(value = "class_title", access = JsonProperty.Access.READ_ONLY)
        String classTitle,

        @Schema(description = "Name of the course, or title of the training program, the class delivers", nullable = true, example = "Beginner Piano")
        @JsonProperty(value = "course_name", access = JsonProperty.Access.READ_ONLY)
        String courseName,

        @Schema(description = "Session format of the class", nullable = true)
        @JsonProperty(value = "session_format", access = JsonProperty.Access.READ_ONLY)
        SessionFormat sessionFormat,

        @Schema(description = "Delivery location type of the class", nullable = true)
        @JsonProperty(value = "location_type", access = JsonProperty.Access.READ_ONLY)
        LocationType locationType,

        @Schema(description = "When the class meets, from its session templates", nullable = true, example = "Mon & Wed · 9:00–11:00")
        @JsonProperty(value = "schedule_summary", access = JsonProperty.Access.READ_ONLY)
        String scheduleSummary,

        @Schema(description = "Training branch the class is delivered at", nullable = true, format = "uuid")
        @JsonProperty(value = "branch_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID branchUuid,

        @Schema(description = "Name of that branch", nullable = true, example = "Main Campus")
        @JsonProperty(value = "branch_name", access = JsonProperty.Access.READ_ONLY)
        String branchName,

        @Schema(description = "When the student first enrolled in the class (UTC)")
        @JsonProperty(value = "enrolled_at", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime enrolledAt,

        @Schema(description = "Sessions attended as a percentage (0-100) of the student's held sessions, i.e. those with attendance recorded (attended or absent); null when none has been recorded", nullable = true, example = "83.3")
        @JsonProperty(value = "attendance_rate", access = JsonProperty.Access.READ_ONLY)
        Double attendanceRate,

        @Schema(description = "The student's standing in the class: ENROLLED while any session enrolment is live, otherwise RESERVED, WAITLISTED or CANCELLED")
        @JsonProperty(value = "enrollment_status", access = JsonProperty.Access.READ_ONLY)
        EnrollmentStatus enrollmentStatus
) {
}
