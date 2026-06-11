package apps.sarafrika.elimika.classes.dto;

import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.validation.ValidTimeRange;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(
        name = "ClassDefinitionCreateRequest",
        description = "Request payload for creating a class definition and its initial schedule templates"
)
@ValidTimeRange(
        startField = "defaultStartTime",
        endField = "defaultEndTime",
        message = "Class end time must be after start time"
)
@ValidTimeRange(
        startField = "academicPeriodStartDate",
        endField = "academicPeriodEndDate",
        allowEqual = true,
        message = "Academic period end date must be on or after start date"
)
@ValidTimeRange(
        startField = "registrationPeriodStartDate",
        endField = "registrationPeriodEndDate",
        allowEqual = true,
        message = "Registration period end date must be on or after start date"
)
public record ClassDefinitionCreateRequestDTO(

        @Schema(description = "**[REQUIRED]** Title of the class definition.", example = "Introduction to Java Programming")
        @NotBlank(message = "Class title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        @JsonProperty("title")
        String title,

        @Schema(description = "**[OPTIONAL]** Detailed description of the class.", maxLength = 2000)
        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        @JsonProperty("description")
        String description,

        @Schema(description = "**[REQUIRED]** Default instructor UUID for the class.")
        @NotNull(message = "Default instructor UUID is required")
        @JsonProperty("default_instructor_uuid")
        UUID defaultInstructorUuid,

        @Schema(description = "**[OPTIONAL]** Organisation UUID that owns the class.")
        @JsonProperty("organisation_uuid")
        UUID organisationUuid,

        @Schema(description = "**[OPTIONAL]** Course UUID for course-scoped classes.")
        @JsonProperty("course_uuid")
        UUID courseUuid,

        @Schema(description = "**[OPTIONAL]** Program UUID for program-scoped classes.")
        @JsonProperty("program_uuid")
        UUID programUuid,

        @Schema(description = "**[OPTIONAL]** Training fee for the class.", minimum = "0")
        @DecimalMin(value = "0.00", message = "Training fee cannot be negative")
        @JsonProperty("training_fee")
        BigDecimal trainingFee,

        @Schema(description = "**[REQUIRED]** Class visibility.", allowableValues = {"PUBLIC", "PRIVATE"})
        @NotNull(message = "Class visibility is required")
        @JsonProperty("class_visibility")
        ClassVisibility classVisibility,

        @Schema(description = "**[REQUIRED]** Session format.", allowableValues = {"INDIVIDUAL", "GROUP"})
        @NotNull(message = "Session format is required")
        @JsonProperty("session_format")
        SessionFormat sessionFormat,

        @Schema(description = "**[REQUIRED]** Default start date-time for the class.", format = "date-time")
        @NotNull(message = "Default start time is required")
        @JsonProperty("default_start_time")
        LocalDateTime defaultStartTime,

        @Schema(description = "**[REQUIRED]** Default end date-time for the class.", format = "date-time")
        @NotNull(message = "Default end time is required")
        @JsonProperty("default_end_time")
        LocalDateTime defaultEndTime,

        @Schema(description = "**[OPTIONAL]** Academic period start date.", format = "date")
        @JsonProperty("academic_period_start_date")
        LocalDate academicPeriodStartDate,

        @Schema(description = "**[OPTIONAL]** Academic period end date.", format = "date")
        @JsonProperty("academic_period_end_date")
        LocalDate academicPeriodEndDate,

        @Schema(description = "**[OPTIONAL]** Registration period start date.", format = "date")
        @JsonProperty("registration_period_start_date")
        LocalDate registrationPeriodStartDate,

        @Schema(description = "**[OPTIONAL]** Registration period end date.", format = "date")
        @JsonProperty("registration_period_end_date")
        LocalDate registrationPeriodEndDate,

        @Schema(description = "**[OPTIONAL]** Reminder lead time in minutes.", minimum = "0")
        @PositiveOrZero(message = "Class reminder minutes cannot be negative")
        @JsonProperty("class_reminder_minutes")
        Integer classReminderMinutes,

        @Schema(description = "**[OPTIONAL]** Hex color used in calendar UI.", example = "#1F6FEB")
        @Pattern(
                regexp = "^#[0-9A-Fa-f]{6}$",
                message = "Class color must be a valid hex color code in the format #RRGGBB"
        )
        @JsonProperty("class_color")
        String classColor,

        @Schema(description = "**[REQUIRED]** Delivery location type.", allowableValues = {"ONLINE", "IN_PERSON", "HYBRID"})
        @NotNull(message = "Location type is required")
        @JsonProperty("location_type")
        LocationType locationType,

        @Schema(description = "**[OPTIONAL]** Human-readable location name.")
        @JsonProperty("location_name")
        String locationName,

        @Schema(description = "**[OPTIONAL]** Location latitude.")
        @JsonProperty("location_latitude")
        BigDecimal locationLatitude,

        @Schema(description = "**[OPTIONAL]** Location longitude.")
        @JsonProperty("location_longitude")
        BigDecimal locationLongitude,

        @Schema(description = "**[OPTIONAL]** Online meeting URL.", maxLength = 1000)
        @Size(max = 1000, message = "Meeting link must not exceed 1000 characters")
        @JsonProperty("meeting_link")
        String meetingLink,

        @Schema(description = "**[OPTIONAL]** Maximum participants.", minimum = "1")
        @Positive(message = "Max participants must be positive")
        @JsonProperty("max_participants")
        Integer maxParticipants,

        @Schema(description = "**[OPTIONAL]** Whether waitlisting is allowed.")
        @JsonProperty("allow_waitlist")
        Boolean allowWaitlist,

        @Schema(description = "**[OPTIONAL]** Whether the class is active.")
        @JsonProperty("is_active")
        Boolean isActive,

        @Schema(description = "**[REQUIRED]** Schedule templates used to generate initial scheduled sessions.")
        @Valid
        @NotNull(message = "session_templates is required")
        @Size(min = 1, message = "At least one session template is required")
        @JsonProperty("session_templates")
        List<ClassSessionTemplateDTO> sessionTemplates
) {

    public ClassDefinitionDTO toClassDefinitionDTO() {
        return toClassDefinitionDTO(courseUuid, programUuid);
    }

    public ClassDefinitionDTO toClassDefinitionDTO(UUID effectiveCourseUuid, UUID effectiveProgramUuid) {
        return new ClassDefinitionDTO(
                null,
                title,
                description,
                defaultInstructorUuid,
                organisationUuid,
                effectiveCourseUuid,
                effectiveProgramUuid,
                trainingFee,
                classVisibility,
                sessionFormat,
                defaultStartTime,
                defaultEndTime,
                academicPeriodStartDate,
                academicPeriodEndDate,
                registrationPeriodStartDate,
                registrationPeriodEndDate,
                classReminderMinutes,
                classColor,
                locationType,
                locationName,
                locationLatitude,
                locationLongitude,
                meetingLink,
                maxParticipants,
                allowWaitlist,
                isActive,
                sessionTemplates,
                null,
                null,
                null,
                null
        );
    }
}
