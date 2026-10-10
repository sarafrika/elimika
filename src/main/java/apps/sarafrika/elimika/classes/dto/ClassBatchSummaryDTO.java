package apps.sarafrika.elimika.classes.dto;

import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** One batch-lookup row: a class with course title, instructor and seats joined, one request not 3N. */
@Schema(name = "ClassBatchSummary", description = "A class definition with its course title, instructor summary, "
        + "enrolment count and seat capacity, as returned by the batch class lookup")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClassBatchSummaryDTO(
        @Schema(description = "Class definition UUID", format = "uuid")
        @JsonProperty("uuid") UUID uuid,

        @Schema(description = "Class title")
        @JsonProperty("title") String title,

        @Schema(description = "Public thumbnail URL", nullable = true)
        @JsonProperty("thumbnail_url") String thumbnailUrl,

        @Schema(description = "Course the class delivers", format = "uuid", nullable = true)
        @JsonProperty("course_uuid") UUID courseUuid,

        @Schema(description = "Title of the course the class delivers", nullable = true)
        @JsonProperty("course_title") String courseTitle,

        @Schema(description = "Training program the class delivers", format = "uuid", nullable = true)
        @JsonProperty("program_uuid") UUID programUuid,

        @Schema(description = "Title of the training program the class delivers", nullable = true)
        @JsonProperty("program_title") String programTitle,

        @Schema(description = "Organisation that owns the class", format = "uuid", nullable = true)
        @JsonProperty("organisation_uuid") UUID organisationUuid,

        @Schema(description = "Default instructor", format = "uuid", nullable = true)
        @JsonProperty("default_instructor_uuid") UUID defaultInstructorUuid,

        @Schema(description = "Directory summary of the default instructor", nullable = true)
        @JsonProperty("instructor") InstructorSummary instructor,

        @Schema(description = "Whether the class is active")
        @JsonProperty("is_active") Boolean isActive,

        @Schema(description = "Class visibility", nullable = true)
        @JsonProperty("class_visibility") ClassVisibility classVisibility,

        @Schema(description = "Delivery location type", nullable = true)
        @JsonProperty("location_type") LocationType locationType,

        @Schema(description = "Session format", nullable = true)
        @JsonProperty("session_format") SessionFormat sessionFormat,

        @Schema(description = "Default session start", nullable = true)
        @JsonProperty("default_start_time") LocalDateTime defaultStartTime,

        @Schema(description = "Default session end", nullable = true)
        @JsonProperty("default_end_time") LocalDateTime defaultEndTime,

        @Schema(description = "Public sale price per seat", nullable = true)
        @JsonProperty("sale_price") BigDecimal salePrice,

        @Schema(description = "Seat capacity of the class", nullable = true)
        @JsonProperty("max_participants") Integer maxParticipants,

        @Schema(description = "Whether a full class accepts a waitlist", nullable = true)
        @JsonProperty("allow_waitlist") Boolean allowWaitlist,

        @Schema(description = "**[PARTIES ONLY]** Distinct actively-enrolled students; present only for the class's "
                + "instructor, managers of its organisation and platform admins", nullable = true)
        @JsonProperty("enrolled_count") Long enrolledCount,

        @Schema(description = "**[PARTIES ONLY]** Seats left (capacity minus enrolled, never below zero); present "
                + "only alongside enrolled_count", nullable = true)
        @JsonProperty("seats_remaining") Long seatsRemaining
) {

    /** What a class row needs to show about its instructor. */
    @Schema(name = "ClassBatchInstructorSummary", description = "Directory summary of a class's instructor")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InstructorSummary(
            @Schema(description = "Instructor UUID", format = "uuid")
            @JsonProperty("uuid") UUID uuid,

            @Schema(description = "Instructor display name")
            @JsonProperty("display_name") String displayName,

            @Schema(description = "Whether an administrator has verified the instructor")
            @JsonProperty("admin_verified") boolean adminVerified
    ) {
    }
}
