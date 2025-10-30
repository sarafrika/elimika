package apps.sarafrika.elimika.classes.dto;

import apps.sarafrika.elimika.classes.util.enums.ClassAssessmentReleaseStrategy;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "ClassAssignmentSchedule",
        description = "Class-level assignment schedule with visibility windows and overrides",
        example = """
        {
            "uuid": "cas-1234-5678-90ab-cdef12345678",
            "class_definition_uuid": "cd123456-7890-abcd-ef01-234567890abc",
            "lesson_uuid": "lesson-1234-5678-90ab-cdef12345678",
            "assignment_uuid": "assign-1234-5678-90ab-cdef12345678",
            "class_lesson_plan_uuid": "clp-1234-5678-90ab-cdef12345678",
            "visible_at": "2024-05-08T07:00:00",
            "due_at": "2024-05-12T23:59:00",
            "grading_due_at": "2024-05-15T17:00:00",
            "timezone": "Africa/Nairobi",
            "release_strategy": "CUSTOM",
            "max_attempts": 2,
            "instructor_uuid": "inst1234-5678-90ab-cdef123456789abc",
            "notes": "Extend due date for the Nairobi cohort.",
            "created_date": "2024-04-01T12:00:00",
            "created_by": "instructor@sarafrika.com",
            "updated_date": "2024-04-03T09:15:00",
            "updated_by": "instructor@sarafrika.com"
        }
        """
)
public record ClassAssignmentScheduleDTO(

        @Schema(
                description = "**[READ-ONLY]** Unique identifier for this class assignment schedule.",
                example = "cas-1234-5678-90ab-cdef12345678",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(
                description = "**[REQUIRED]** Class definition that owns the assignment schedule.",
                example = "cd123456-7890-abcd-ef01-234567890abc"
        )
        @JsonProperty("class_definition_uuid")
        UUID classDefinitionUuid,

        @Schema(
                description = "**[REQUIRED]** Lesson this assignment is associated with for the class.",
                example = "lesson-1234-5678-90ab-cdef12345678"
        )
        @JsonProperty("lesson_uuid")
        UUID lessonUuid,

        @Schema(
                description = "**[REQUIRED]** Assignment template or clone that the schedule references.",
                example = "assign-1234-5678-90ab-cdef12345678"
        )
        @JsonProperty("assignment_uuid")
        UUID assignmentUuid,

        @Schema(
                description = "**[OPTIONAL]** Lesson plan entry this schedule ties to.",
                example = "clp-1234-5678-90ab-cdef12345678"
        )
        @JsonProperty("class_lesson_plan_uuid")
        UUID classLessonPlanUuid,

        @Schema(
                description = "**[OPTIONAL]** When the assignment becomes visible to students (UTC).",
                example = "2024-05-08T07:00:00"
        )
        @JsonProperty("visible_at")
        LocalDateTime visibleAt,

        @Schema(
                description = "**[OPTIONAL]** Submission deadline for the class (UTC).",
                example = "2024-05-12T23:59:00"
        )
        @JsonProperty("due_at")
        LocalDateTime dueAt,

        @Schema(
                description = "**[OPTIONAL]** Deadline for trainers to complete grading (UTC).",
                example = "2024-05-15T17:00:00"
        )
        @JsonProperty("grading_due_at")
        LocalDateTime gradingDueAt,

        @Schema(
                description = "**[OPTIONAL]** IANA timezone identifier used when displaying deadlines.",
                example = "Africa/Nairobi"
        )
        @JsonProperty("timezone")
        String timezone,

        @Schema(
                description = "**[REQUIRED]** Strategy describing how this class schedule derives from the template.",
                example = "CUSTOM",
                allowableValues = {"INHERITED", "CUSTOM", "CLONE"}
        )
        @JsonProperty("release_strategy")
        ClassAssessmentReleaseStrategy releaseStrategy,

        @Schema(
                description = "**[OPTIONAL]** Maximum attempts allowed for this class schedule (overrides template).",
                example = "2"
        )
        @JsonProperty("max_attempts")
        Integer maxAttempts,

        @Schema(
                description = "**[REQUIRED]** Instructor responsible for this assignment schedule.",
                example = "inst1234-5678-90ab-cdef123456789abc"
        )
        @JsonProperty("instructor_uuid")
        UUID instructorUuid,

        @Schema(
                description = "**[OPTIONAL]** Instructor notes shown internally for context.",
                example = "Extend due date for the Nairobi cohort."
        )
        @JsonProperty("notes")
        String notes,

        @Schema(
                description = "**[READ-ONLY]** Timestamp when the schedule was created.",
                example = "2024-04-01T12:00:00",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @Schema(
                description = "**[READ-ONLY]** User identifier who created the schedule.",
                example = "instructor@sarafrika.com",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy,

        @Schema(
                description = "**[READ-ONLY]** Timestamp when the schedule was last updated.",
                example = "2024-04-03T09:15:00",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedDate,

        @Schema(
                description = "**[READ-ONLY]** User identifier who last updated the schedule.",
                example = "instructor@sarafrika.com",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        String updatedBy
) {
}
