package apps.sarafrika.elimika.classes.dto;

import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(
        name = "ClassMarketplaceJob",
        description = "Marketplace job advert for an organisation-owned class awaiting instructor assignment"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClassMarketplaceJobDTO(

        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @JsonProperty(value = "organisation_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID organisationUuid,

        @JsonProperty(value = "course_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID courseUuid,

        @JsonProperty(value = "title", access = JsonProperty.Access.READ_ONLY)
        String title,

        @JsonProperty(value = "description", access = JsonProperty.Access.READ_ONLY)
        String description,

        @JsonProperty(value = "status", access = JsonProperty.Access.READ_ONLY)
        ClassMarketplaceJobStatus status,

        @JsonProperty(value = "class_visibility", access = JsonProperty.Access.READ_ONLY)
        ClassVisibility classVisibility,

        @JsonProperty(value = "session_format", access = JsonProperty.Access.READ_ONLY)
        SessionFormat sessionFormat,

        @JsonProperty(value = "default_start_time", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime defaultStartTime,

        @JsonProperty(value = "default_end_time", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime defaultEndTime,

        @JsonProperty(value = "academic_period_start_date", access = JsonProperty.Access.READ_ONLY)
        LocalDate academicPeriodStartDate,

        @JsonProperty(value = "academic_period_end_date", access = JsonProperty.Access.READ_ONLY)
        LocalDate academicPeriodEndDate,

        @JsonProperty(value = "registration_period_start_date", access = JsonProperty.Access.READ_ONLY)
        LocalDate registrationPeriodStartDate,

        @JsonProperty(value = "registration_period_end_date", access = JsonProperty.Access.READ_ONLY)
        LocalDate registrationPeriodEndDate,

        @JsonProperty(value = "class_reminder_minutes", access = JsonProperty.Access.READ_ONLY)
        Integer classReminderMinutes,

        @JsonProperty(value = "class_color", access = JsonProperty.Access.READ_ONLY)
        String classColor,

        @JsonProperty(value = "location_type", access = JsonProperty.Access.READ_ONLY)
        LocationType locationType,

        @JsonProperty(value = "location_name", access = JsonProperty.Access.READ_ONLY)
        String locationName,

        @JsonProperty(value = "location_latitude", access = JsonProperty.Access.READ_ONLY)
        BigDecimal locationLatitude,

        @JsonProperty(value = "location_longitude", access = JsonProperty.Access.READ_ONLY)
        BigDecimal locationLongitude,

        @JsonProperty(value = "meeting_link", access = JsonProperty.Access.READ_ONLY)
        String meetingLink,

        @JsonProperty(value = "max_participants", access = JsonProperty.Access.READ_ONLY)
        Integer maxParticipants,

        @JsonProperty(value = "allow_waitlist", access = JsonProperty.Access.READ_ONLY)
        Boolean allowWaitlist,

        @JsonProperty(value = "assigned_instructor_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID assignedInstructorUuid,

        @JsonProperty(value = "assigned_application_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID assignedApplicationUuid,

        @JsonProperty(value = "assigned_class_definition_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID assignedClassDefinitionUuid,

        @JsonProperty(value = "filled_at", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime filledAt,

        @JsonProperty(value = "session_templates", access = JsonProperty.Access.READ_ONLY)
        List<ClassSessionTemplateDTO> sessionTemplates,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedDate,

        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy,

        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        String updatedBy
) {

    @JsonProperty(value = "duration_minutes", access = JsonProperty.Access.READ_ONLY)
    public long getDurationMinutes() {
        if (defaultStartTime == null || defaultEndTime == null) {
            return 0;
        }
        return java.time.Duration.between(defaultStartTime, defaultEndTime).toMinutes();
    }
}
