package apps.sarafrika.elimika.classes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "ClassLessonPlan",
        description = "Lesson scheduling metadata scoped to a class definition",
        example = """
        {
            "uuid": "clp-1234-5678-90ab-cdef12345678",
            "class_definition_uuid": "cd123456-7890-abcd-ef01-234567890abc",
            "lesson_uuid": "lesson-1234-5678-90ab-cdef12345678",
            "scheduled_start": "2024-05-10T09:00:00",
            "scheduled_end": "2024-05-10T10:30:00",
            "scheduled_instance_uuid": "si123456-7890-abcd-ef01-234567890abc",
            "instructor_uuid": "inst1234-5678-90ab-cdef123456789abc",
            "notes": "Cover prerequisite concepts from the bootcamp cohort.",
            "created_date": "2024-04-01T12:00:00",
            "created_by": "instructor@sarafrika.com",
            "updated_date": "2024-04-02T08:30:00",
            "updated_by": "instructor@sarafrika.com"
        }
        """
)
public record ClassLessonPlanDTO(

        @Schema(
                description = "**[READ-ONLY]** Unique identifier for this class lesson plan entry.",
                example = "clp-1234-5678-90ab-cdef12345678",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(
                description = "**[REQUIRED]** Class definition that owns this plan entry.",
                example = "cd123456-7890-abcd-ef01-234567890abc"
        )
        @JsonProperty("class_definition_uuid")
        UUID classDefinitionUuid,

        @Schema(
                description = "**[REQUIRED]** Lesson the plan entry references.",
                example = "lesson-1234-5678-90ab-cdef12345678"
        )
        @JsonProperty("lesson_uuid")
        UUID lessonUuid,

        @Schema(
                description = "**[OPTIONAL]** Planned start timestamp in UTC.",
                example = "2024-05-10T09:00:00"
        )
        @JsonProperty("scheduled_start")
        LocalDateTime scheduledStart,

        @Schema(
                description = "**[OPTIONAL]** Planned end timestamp in UTC.",
                example = "2024-05-10T10:30:00"
        )
        @JsonProperty("scheduled_end")
        LocalDateTime scheduledEnd,

        @Schema(
                description = "**[OPTIONAL]** Reference to a concrete scheduled instance created by timetabling.",
                example = "si123456-7890-abcd-ef01-234567890abc"
        )
        @JsonProperty("scheduled_instance_uuid")
        UUID scheduledInstanceUuid,

        @Schema(
                description = "**[OPTIONAL]** Instructor assigned to deliver this lesson.",
                example = "inst1234-5678-90ab-cdef123456789abc"
        )
        @JsonProperty("instructor_uuid")
        UUID instructorUuid,

        @Schema(
                description = "**[OPTIONAL]** Trainer notes or reminders for the lesson.",
                example = "Cover prerequisite concepts from the bootcamp cohort."
        )
        @JsonProperty("notes")
        String notes,

        @Schema(
                description = "**[READ-ONLY]** Timestamp when this plan entry was created.",
                example = "2024-04-01T12:00:00",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @Schema(
                description = "**[READ-ONLY]** User identifier who created the plan entry.",
                example = "instructor@sarafrika.com",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy,

        @Schema(
                description = "**[READ-ONLY]** Timestamp when the plan entry was last updated.",
                example = "2024-04-02T08:30:00",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedDate,

        @Schema(
                description = "**[READ-ONLY]** User identifier who last updated the plan entry.",
                example = "instructor@sarafrika.com",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        String updatedBy
) {
}
