package apps.sarafrika.elimika.classes.dto;

import apps.sarafrika.elimika.shared.enums.ClassServiceType;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.validation.ValidTimeRange;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
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
        name = "ClassMarketplaceJobRequest",
        description = "Draft class advert posted by an organisation before a final instructor is assigned",
        example = """
                {
                  "organisation_uuid": "org-1234-5678-90ab-cdef12345678",
                  "course_uuid": "course-1234-5678-90ab-cdef12345678",
                  "title": "Weekend Data Analysis Bootcamp",
                  "description": "School-led advert for an approved course delivery slot.",
                  "class_visibility": "PUBLIC",
                  "session_format": "GROUP",
                  "default_start_time": "2026-05-02T09:00:00",
                  "default_end_time": "2026-05-02T12:00:00",
                  "location_type": "HYBRID",
                  "location_name": "Nairobi Campus - Lab 2",
                  "location_latitude": -1.292066,
                  "location_longitude": 36.821945,
                  "meeting_link": "https://meet.google.com/abc-defg-hij",
                  "max_participants": 24,
                  "allow_waitlist": true,
                  "session_templates": [
                    {
                      "start_time": "2026-05-02T09:00:00",
                      "end_time": "2026-05-02T12:00:00",
                      "recurrence": {
                        "recurrence_type": "WEEKLY",
                        "interval_value": 1,
                        "days_of_week": "SATURDAY",
                        "occurrence_count": 6
                      },
                      "conflict_resolution": "FAIL"
                    }
                  ]
                }
                """
)
@JsonInclude(JsonInclude.Include.NON_NULL)
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
public record ClassMarketplaceJobRequestDTO(

        @Schema(description = "**[REQUIRED]** Organisation posting the marketplace class job.", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("organisation_uuid")
        @NotNull(message = "organisation_uuid is required")
        UUID organisationUuid,

        @Schema(description = "**[OPTIONAL]** Course backing the advertised class. Required when program_uuid is not provided.", nullable = true)
        @JsonProperty("course_uuid")
        UUID courseUuid,

        @Schema(description = "**[OPTIONAL]** Training program backing the advertised class. Required when course_uuid is not provided.", nullable = true)
        @JsonProperty("program_uuid")
        UUID programUuid,

        @Schema(description = "**[REQUIRED]** Advert title for the class job.", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("title")
        @NotBlank(message = "title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @Schema(description = "Optional description for instructors evaluating the class job.", nullable = true)
        @JsonProperty("description")
        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        @Schema(description = "**[REQUIRED]** Visibility that will be used if the class is finally created.", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("class_visibility")
        @NotNull(message = "class_visibility is required")
        ClassVisibility classVisibility,

        @Schema(description = "**[REQUIRED]** Session format that the assigned instructor will deliver.", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("session_format")
        @NotNull(message = "session_format is required")
        SessionFormat sessionFormat,

        @Schema(description = "**[REQUIRED]** Default start date-time for the advertised class (UTC).", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("default_start_time")
        @NotNull(message = "default_start_time is required")
        LocalDateTime defaultStartTime,

        @Schema(description = "**[OPTIONAL]** Default end date-time for the advertised class (UTC). If duration_minutes is supplied, the backend derives this value.", nullable = true)
        @JsonProperty("default_end_time")
        LocalDateTime defaultEndTime,

        @Schema(description = "**[REQUIRED]** Positive default class duration in minutes. When supplied it is authoritative and the backend derives default_end_time from default_start_time.", minimum = "1", example = "180")
        @JsonProperty("duration_minutes")
        @Positive(message = "duration_minutes must be positive")
        Integer durationMinutes,

        @Schema(description = "Optional academic period start date.", nullable = true)
        @JsonProperty("academic_period_start_date")
        LocalDate academicPeriodStartDate,

        @Schema(description = "Optional academic period end date.", nullable = true)
        @JsonProperty("academic_period_end_date")
        LocalDate academicPeriodEndDate,

        @Schema(description = "Optional registration period start date.", nullable = true)
        @JsonProperty("registration_period_start_date")
        LocalDate registrationPeriodStartDate,

        @Schema(description = "Optional registration period end date.", nullable = true)
        @JsonProperty("registration_period_end_date")
        LocalDate registrationPeriodEndDate,

        @Schema(description = "Optional reminder lead time in minutes.", nullable = true)
        @JsonProperty("class_reminder_minutes")
        @PositiveOrZero(message = "Class reminder minutes cannot be negative")
        Integer classReminderMinutes,

        @Schema(description = "Optional UI color for the class advert.", nullable = true)
        @JsonProperty("class_color")
        @Pattern(
                regexp = "^#[0-9A-Fa-f]{6}$",
                message = "Class color must be a valid hex color code in the format #RRGGBB"
        )
        String classColor,

        @Schema(description = "**[REQUIRED]** Delivery location type for the advertised class.", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("location_type")
        @NotNull(message = "location_type is required")
        LocationType locationType,

        @Schema(description = "Optional human-readable location name. Required for IN_PERSON and HYBRID.", nullable = true)
        @JsonProperty("location_name")
        @Size(max = 255, message = "Location name must not exceed 255 characters")
        String locationName,

        @Schema(description = "Optional location latitude. Required for IN_PERSON and HYBRID.", nullable = true)
        @JsonProperty("location_latitude")
        BigDecimal locationLatitude,

        @Schema(description = "Optional location longitude. Required for IN_PERSON and HYBRID.", nullable = true)
        @JsonProperty("location_longitude")
        BigDecimal locationLongitude,

        @Schema(description = "Optional virtual meeting link for ONLINE or HYBRID delivery.", nullable = true)
        @JsonProperty("meeting_link")
        @Size(max = 1000, message = "Meeting link must not exceed 1000 characters")
        String meetingLink,

        @Schema(description = "Optional maximum participant count.", nullable = true)
        @JsonProperty("max_participants")
        @Positive(message = "Max participants must be positive")
        Integer maxParticipants,

        @Schema(description = "Optional waitlist toggle for the eventual class.", nullable = true)
        @JsonProperty("allow_waitlist")
        Boolean allowWaitlist,

        @Schema(description = "**[OPTIONAL]** Price per learner per hour, charged once the class exists. Defaults to the organisation's approved rate when omitted. Must be at least the course minimum training fee.", nullable = true, example = "240.00")
        @JsonProperty("sale_price")
        @PositiveOrZero(message = "Sale price cannot be negative")
        BigDecimal salePrice,

        @Schema(description = "**[OPTIONAL]** Per-session pay offered to the eventual instructor. An applicant is assignable only when this is at least their approved rate. Defaults to the sale price when omitted, leaving no margin.", nullable = true, example = "180.00")
        @JsonProperty("instructor_pay")
        @PositiveOrZero(message = "Instructor pay cannot be negative")
        BigDecimal instructorPay,

        @Schema(description = "**[OPTIONAL]** Unit both prices are quoted in, fixed by the contract this job represents. Defaults to per hour, which is what every existing price means.", nullable = true, example = "per_hour")
        @JsonProperty("rate_basis")
        apps.sarafrika.elimika.shared.utils.enums.RateBasis rateBasis,

        @Schema(description = "**[REQUIRED]** Session templates that will be used when the class is assigned and created.", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("session_templates")
        @NotNull(message = "session_templates is required")
        @Size(min = 1, message = "At least one session template is required")
        @Valid
        List<ClassSessionTemplateDTO> sessionTemplates,

        @Schema(description = "**[OPTIONAL]** Organisation resources (venue, equipment pools) to reserve for every session while recruitment runs. At most one venue; posting fails with a per-occurrence conflict report if any resource is unavailable.", nullable = true)
        @JsonProperty("resources")
        @Valid
        List<ClassMarketplaceJobResourceDTO> resources,

        @Schema(description = "**[OPTIONAL]** Preset service the class is offered under (drives the commercial format shown to learners).", nullable = true)
        @JsonProperty("service_type")
        ClassServiceType serviceType,

        @Schema(description = "**[OPTIONAL]** Instructor to assign at creation. When provided, the class is materialised and assigned to this instructor immediately (the job is filled, no open application step); when omitted, the class is posted for instructors to apply.", nullable = true)
        @JsonProperty("preferred_instructor_uuid")
        UUID preferredInstructorUuid,

        @Schema(description = "**[OPTIONAL]** Name snapshot of the target learner groups. Ignored when target_group_uuids is supplied \u2014 the names are then resolved from the organisation's student groups.", nullable = true)
        @JsonProperty("target_groups")
        List<String> targetGroups,

        @Schema(description = "**[OPTIONAL]** Organisation student groups (cohorts / streams) the class is aimed at. Identifiers that do not belong to the owning organisation are rejected.", nullable = true)
        @JsonProperty("target_group_uuids")
        List<UUID> targetGroupUuids,

        @Schema(description = "**[OPTIONAL]** Category the class falls under. Course-backed classes inherit the course's own categories, so this is intended for program-backed classes which carry none per class.", nullable = true)
        @JsonProperty("category_uuid")
        UUID categoryUuid,

        @Schema(description = "**[OPTIONAL]** Send session reminders to enrolled students.", nullable = true)
        @JsonProperty("remind_students")
        Boolean remindStudents,

        @Schema(description = "**[OPTIONAL]** Send session reminders to the assigned instructor.", nullable = true)
        @JsonProperty("remind_instructor")
        Boolean remindInstructor,

        @Schema(description = "**[OPTIONAL]** Deliver reminders via email.", nullable = true)
        @JsonProperty("remind_via_email")
        Boolean remindViaEmail,

        @Schema(description = "**[OPTIONAL]** Deliver reminders via SMS.", nullable = true)
        @JsonProperty("remind_via_sms")
        Boolean remindViaSms,

        @Schema(description = "**[OPTIONAL]** Deliver reminders via push notification.", nullable = true)
        @JsonProperty("remind_via_push")
        Boolean remindViaPush
) {

    @JsonIgnore
    @AssertTrue(message = "Exactly one of course_uuid or program_uuid is required")
    public boolean hasSingleLearningContext() {
        return (courseUuid == null) != (programUuid == null);
    }

    @JsonIgnore
    @AssertTrue(message = "Either default_end_time or duration_minutes is required")
    public boolean hasDefaultEndTimeOrDuration() {
        return defaultEndTime != null || durationMinutes != null;
    }

    @JsonIgnore
    @AssertTrue(message = "Class end time must be after start time")
    public boolean hasValidDefaultEndTimeWhenDurationMissing() {
        if (durationMinutes != null || defaultStartTime == null || defaultEndTime == null) {
            return true;
        }
        return defaultStartTime.isBefore(defaultEndTime);
    }

    public ClassMarketplaceJobRequestDTO withDurationApplied() {
        LocalDateTime effectiveDefaultEndTime = defaultEndTime;
        if (defaultStartTime != null && durationMinutes != null) {
            effectiveDefaultEndTime = defaultStartTime.plusMinutes(durationMinutes.longValue());
        }

        List<ClassSessionTemplateDTO> effectiveSessionTemplates = sessionTemplates == null
                ? null
                : sessionTemplates.stream()
                .map(template -> template == null ? null : template.withDurationApplied(durationMinutes))
                .toList();

        return new ClassMarketplaceJobRequestDTO(
                organisationUuid,
                courseUuid,
                programUuid,
                title,
                description,
                classVisibility,
                sessionFormat,
                defaultStartTime,
                effectiveDefaultEndTime,
                durationMinutes,
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
                salePrice,
                instructorPay,
                rateBasis,
                effectiveSessionTemplates,
                resources,
                serviceType,
                preferredInstructorUuid,
                targetGroups,
                targetGroupUuids,
                categoryUuid,
                remindStudents,
                remindInstructor,
                remindViaEmail,
                remindViaSms,
                remindViaPush
        );
    }
}
