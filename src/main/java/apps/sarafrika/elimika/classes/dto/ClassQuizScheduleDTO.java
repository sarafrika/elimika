package apps.sarafrika.elimika.classes.dto;

import apps.sarafrika.elimika.classes.util.enums.ClassAssessmentReleaseStrategy;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "ClassQuizSchedule",
        description = "Class-level quiz schedule with release timing and override values",
        example = """
        {
            "uuid": "cqs-1234-5678-90ab-cdef12345678",
            "class_definition_uuid": "cd123456-7890-abcd-ef01-234567890abc",
            "lesson_uuid": "lesson-1234-5678-90ab-cdef12345678",
            "quiz_uuid": "quiz-1234-5678-90ab-cdef12345678",
            "class_lesson_plan_uuid": "clp-1234-5678-90ab-cdef12345678",
            "visible_at": "2024-05-09T07:00:00",
            "due_at": "2024-05-09T23:59:00",
            "timezone": "Africa/Nairobi",
            "release_strategy": "CUSTOM",
            "time_limit_override": 45,
            "attempt_limit_override": 2,
            "passing_score_override": 75.50,
            "instructor_uuid": "inst1234-5678-90ab-cdef123456789abc",
            "notes": "Extend time limit for the evening session.",
            "created_date": "2024-04-01T12:00:00",
            "created_by": "instructor@sarafrika.com",
            "updated_date": "2024-04-03T10:00:00",
            "updated_by": "instructor@sarafrika.com"
        }
        """
)
public record ClassQuizScheduleDTO(

        @Schema(
                description = "**[READ-ONLY]** Unique identifier for this class quiz schedule.",
                example = "cqs-1234-5678-90ab-cdef12345678",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(
                description = "**[REQUIRED]** Class definition that owns the quiz schedule.",
                example = "cd123456-7890-abcd-ef01-234567890abc"
        )
        @JsonProperty("class_definition_uuid")
        UUID classDefinitionUuid,

        @Schema(
                description = "**[REQUIRED]** Lesson associated with the quiz for this class.",
                example = "lesson-1234-5678-90ab-cdef12345678"
        )
        @JsonProperty("lesson_uuid")
        UUID lessonUuid,

        @Schema(
                description = "**[REQUIRED]** Quiz template or clone referenced by this schedule.",
                example = "quiz-1234-5678-90ab-cdef12345678"
        )
        @JsonProperty("quiz_uuid")
        UUID quizUuid,

        @Schema(
                description = "**[OPTIONAL]** Linked lesson plan entry for ordering context.",
                example = "clp-1234-5678-90ab-cdef12345678"
        )
        @JsonProperty("class_lesson_plan_uuid")
        UUID classLessonPlanUuid,

        @Schema(
                description = "**[OPTIONAL]** When the quiz is visible to students (UTC).",
                example = "2024-05-09T07:00:00"
        )
        @JsonProperty("visible_at")
        LocalDateTime visibleAt,

        @Schema(
                description = "**[OPTIONAL]** Deadline for completing the quiz (UTC).",
                example = "2024-05-09T23:59:00"
        )
        @JsonProperty("due_at")
        LocalDateTime dueAt,

        @Schema(
                description = "**[OPTIONAL]** IANA timezone identifier for display purposes.",
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
                description = "**[OPTIONAL]** Overrides the quiz time limit (minutes) for this class.",
                example = "45"
        )
        @JsonProperty("time_limit_override")
        Integer timeLimitOverride,

        @Schema(
                description = "**[OPTIONAL]** Overrides the number of attempts allowed.",
                example = "2"
        )
        @JsonProperty("attempt_limit_override")
        Integer attemptLimitOverride,

        @Schema(
                description = "**[OPTIONAL]** Overrides the passing score requirement.",
                example = "75.50"
        )
        @JsonProperty("passing_score_override")
        BigDecimal passingScoreOverride,

        @Schema(
                description = "**[REQUIRED]** Instructor responsible for this quiz schedule.",
                example = "inst1234-5678-90ab-cdef123456789abc"
        )
        @JsonProperty("instructor_uuid")
        UUID instructorUuid,

        @Schema(
                description = "**[OPTIONAL]** Instructor notes for this quiz schedule.",
                example = "Extend time limit for the evening session."
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
                example = "2024-04-03T10:00:00",
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
