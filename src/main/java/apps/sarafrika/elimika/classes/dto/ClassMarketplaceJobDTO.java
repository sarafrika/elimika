package apps.sarafrika.elimika.classes.dto;

import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobStatus;
import apps.sarafrika.elimika.shared.enums.ClassServiceType;
import apps.sarafrika.elimika.shared.enums.ClassVisibility;
import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import apps.sarafrika.elimika.shared.utils.CoordinatePrecision;
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

        @JsonProperty(value = "program_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID programUuid,

        @JsonProperty(value = "title", access = JsonProperty.Access.READ_ONLY)
        String title,

        @JsonProperty(value = "description", access = JsonProperty.Access.READ_ONLY)
        String description,

        @JsonProperty(value = "sale_price", access = JsonProperty.Access.READ_ONLY)
        BigDecimal salePrice,

        @Schema(description = "**[READ-ONLY]** Per-session pay offered to the eventual instructor.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "instructor_pay", access = JsonProperty.Access.READ_ONLY)
        BigDecimal instructorPay,

        @JsonProperty(value = "rate_basis", access = JsonProperty.Access.READ_ONLY)
        apps.sarafrika.elimika.shared.utils.enums.RateBasis rateBasis,

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

        @Schema(description = "Public URL to the class advert thumbnail image, if uploaded.")
        @JsonProperty(value = "thumbnail_url", access = JsonProperty.Access.READ_ONLY)
        String thumbnailUrl,

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

        @JsonProperty(value = "resources", access = JsonProperty.Access.READ_ONLY)
        List<ClassMarketplaceJobResourceDTO> resources,

        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate,

        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime updatedDate,

        @JsonProperty(value = "created_by", access = JsonProperty.Access.READ_ONLY)
        String createdBy,

        @JsonProperty(value = "updated_by", access = JsonProperty.Access.READ_ONLY)
        String updatedBy,

        @JsonProperty(value = "service_type", access = JsonProperty.Access.READ_ONLY)
        ClassServiceType serviceType,

        @JsonProperty(value = "preferred_instructor_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID preferredInstructorUuid,

        @JsonProperty(value = "target_groups", access = JsonProperty.Access.READ_ONLY)
        List<String> targetGroups,

        @JsonProperty(value = "target_group_uuids", access = JsonProperty.Access.READ_ONLY)
        List<UUID> targetGroupUuids,

        @JsonProperty(value = "category_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID categoryUuid,

        @JsonProperty(value = "remind_students", access = JsonProperty.Access.READ_ONLY)
        Boolean remindStudents,

        @JsonProperty(value = "remind_instructor", access = JsonProperty.Access.READ_ONLY)
        Boolean remindInstructor,

        @JsonProperty(value = "remind_via_email", access = JsonProperty.Access.READ_ONLY)
        Boolean remindViaEmail,

        @JsonProperty(value = "remind_via_sms", access = JsonProperty.Access.READ_ONLY)
        Boolean remindViaSms,

        @JsonProperty(value = "remind_via_push", access = JsonProperty.Access.READ_ONLY)
        Boolean remindViaPush,

        @Schema(description = "**[READ-ONLY]** Training branch the class is delivered at (null only on legacy jobs).", accessMode = Schema.AccessMode.READ_ONLY, nullable = true)
        @JsonProperty(value = "branch_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID branchUuid,

        @Schema(description = "**[READ-ONLY]** Name of the job's training branch.", accessMode = Schema.AccessMode.READ_ONLY, nullable = true)
        @JsonProperty(value = "branch_name", access = JsonProperty.Access.READ_ONLY)
        String branchName,

        @Schema(description = "**[READ-ONLY]** Applications received for the job, not counting withdrawn ones.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "application_count", access = JsonProperty.Access.READ_ONLY)
        Long applicationCount,

        @Schema(description = "**[READ-ONLY]** Instructor hired for the job; null until someone is hired.", accessMode = Schema.AccessMode.READ_ONLY, nullable = true)
        @JsonProperty(value = "hired_instructor_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID hiredInstructorUuid,

        @Schema(description = "**[READ-ONLY]** The branch's contact person; only for the hired instructor, the organisation's managers and platform admins.", accessMode = Schema.AccessMode.READ_ONLY, nullable = true)
        @JsonProperty(value = "contact_name", access = JsonProperty.Access.READ_ONLY)
        String contactName,

        @Schema(description = "**[READ-ONLY]** The contact person's phone; same visibility as contact_name.", accessMode = Schema.AccessMode.READ_ONLY, nullable = true)
        @JsonProperty(value = "contact_phone", access = JsonProperty.Access.READ_ONLY)
        String contactPhone,

        @Schema(description = "**[READ-ONLY]** The contact person's email; same visibility as contact_name.", accessMode = Schema.AccessMode.READ_ONLY, nullable = true)
        @JsonProperty(value = "contact_email", access = JsonProperty.Access.READ_ONLY)
        String contactEmail,

        @Schema(description = "**[READ-ONLY]** On a near-me listing (near=lat,lng) only: how far the job is from the searched point, as a coarse band. Never metres.", example = "2-5 km", allowableValues = {"<2 km", "2-5 km", "5-10 km", "10-25 km", ">25 km"}, accessMode = Schema.AccessMode.READ_ONLY, nullable = true)
        @JsonProperty(value = "distance_band", access = JsonProperty.Access.READ_ONLY)
        String distanceBand
) {

    /** Every field but the near-me distance band. */
    public ClassMarketplaceJobDTO(
            UUID uuid,
            UUID organisationUuid,
            UUID courseUuid,
            UUID programUuid,
            String title,
            String description,
            BigDecimal salePrice,
            BigDecimal instructorPay,
            apps.sarafrika.elimika.shared.utils.enums.RateBasis rateBasis,
            ClassMarketplaceJobStatus status,
            ClassVisibility classVisibility,
            SessionFormat sessionFormat,
            LocalDateTime defaultStartTime,
            LocalDateTime defaultEndTime,
            LocalDate academicPeriodStartDate,
            LocalDate academicPeriodEndDate,
            LocalDate registrationPeriodStartDate,
            LocalDate registrationPeriodEndDate,
            Integer classReminderMinutes,
            String classColor,
            String thumbnailUrl,
            LocationType locationType,
            String locationName,
            BigDecimal locationLatitude,
            BigDecimal locationLongitude,
            String meetingLink,
            Integer maxParticipants,
            Boolean allowWaitlist,
            UUID assignedInstructorUuid,
            UUID assignedApplicationUuid,
            UUID assignedClassDefinitionUuid,
            LocalDateTime filledAt,
            List<ClassSessionTemplateDTO> sessionTemplates,
            List<ClassMarketplaceJobResourceDTO> resources,
            LocalDateTime createdDate,
            LocalDateTime updatedDate,
            String createdBy,
            String updatedBy,
            ClassServiceType serviceType,
            UUID preferredInstructorUuid,
            List<String> targetGroups,
            List<UUID> targetGroupUuids,
            UUID categoryUuid,
            Boolean remindStudents,
            Boolean remindInstructor,
            Boolean remindViaEmail,
            Boolean remindViaSms,
            Boolean remindViaPush,
            UUID branchUuid,
            String branchName,
            Long applicationCount,
            UUID hiredInstructorUuid,
            String contactName,
            String contactPhone,
            String contactEmail
    ) {
        this(uuid, organisationUuid, courseUuid, programUuid, title, description, salePrice, instructorPay, rateBasis, status, classVisibility, sessionFormat, defaultStartTime, defaultEndTime, academicPeriodStartDate, academicPeriodEndDate, registrationPeriodStartDate, registrationPeriodEndDate, classReminderMinutes, classColor, thumbnailUrl, locationType, locationName, locationLatitude, locationLongitude, meetingLink, maxParticipants, allowWaitlist, assignedInstructorUuid, assignedApplicationUuid, assignedClassDefinitionUuid, filledAt, sessionTemplates, resources, createdDate, updatedDate, createdBy, updatedBy, serviceType, preferredInstructorUuid, targetGroups, targetGroupUuids, categoryUuid, remindStudents, remindInstructor, remindViaEmail, remindViaSms, remindViaPush, branchUuid, branchName, applicationCount, hiredInstructorUuid, contactName, contactPhone, contactEmail, null);
    }

    /** A near-me row: the distance band, and the job's coordinates rounded to town level. */
    public ClassMarketplaceJobDTO forNearMe(String band) {
        return new ClassMarketplaceJobDTO(uuid, organisationUuid, courseUuid, programUuid, title, description, salePrice, instructorPay, rateBasis, status, classVisibility, sessionFormat, defaultStartTime, defaultEndTime, academicPeriodStartDate, academicPeriodEndDate, registrationPeriodStartDate, registrationPeriodEndDate, classReminderMinutes, classColor, thumbnailUrl, locationType, locationName, CoordinatePrecision.toPublic(locationLatitude), CoordinatePrecision.toPublic(locationLongitude), meetingLink, maxParticipants, allowWaitlist, assignedInstructorUuid, assignedApplicationUuid, assignedClassDefinitionUuid, filledAt, sessionTemplates, resources, createdDate, updatedDate, createdBy, updatedBy, serviceType, preferredInstructorUuid, targetGroups, targetGroupUuids, categoryUuid, remindStudents, remindInstructor, remindViaEmail, remindViaSms, remindViaPush, branchUuid, branchName, applicationCount, hiredInstructorUuid, contactName, contactPhone, contactEmail, band);
    }

    @JsonProperty(value = "duration_minutes", access = JsonProperty.Access.READ_ONLY)
    public long getDurationMinutes() {
        if (defaultStartTime == null || defaultEndTime == null) {
            return 0;
        }
        return java.time.Duration.between(defaultStartTime, defaultEndTime).toMinutes();
    }
}
