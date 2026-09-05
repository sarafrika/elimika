package apps.sarafrika.elimika.classes.factory;

import apps.sarafrika.elimika.classes.dto.ClassDefinitionDTO;
import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.shared.storage.util.FileUrlResolver;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZoneOffset;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClassDefinitionFactory {

    /** Matches the sentinel the backfill migration wrote for classes with no academic period. */
    private static final LocalDate OPEN_ENDED_REGISTRATION = LocalDate.of(2099, 12, 31);

    public static ClassDefinitionDTO toDTO(ClassDefinition entity) {
        if (entity == null) {
            return null;
        }
        return new ClassDefinitionDTO(
                entity.getUuid(),
                entity.getTitle(),
                entity.getDescription(),
                FileUrlResolver.publicUrl(entity.getThumbnailUrl()),
                FileUrlResolver.publicUrl(entity.getPromotionalVideoUrl()),
                entity.getDefaultInstructorUuid(),
                entity.getOrganisationUuid(),
                entity.getBranchUuid(),
                entity.getCourseUuid(),
                entity.getProgramUuid(),
                entity.getSalePrice(),
                entity.getInstructorPay(),
                entity.getRateBasis(),
                entity.getClassVisibility(),
                entity.getSessionFormat(),
                entity.getDefaultStartTime(),
                entity.getDefaultEndTime(),
                entity.getAcademicPeriodStartDate(),
                entity.getAcademicPeriodEndDate(),
                entity.getRegistrationPeriodStartDate(),
                entity.getRegistrationPeriodEndDate(),
                entity.getClassReminderMinutes(),
                entity.getClassColor(),
                entity.getLocationType(),
                entity.getLocationName(),
                entity.getLocationLatitude(),
                entity.getLocationLongitude(),
                entity.getMeetingLink(),
                entity.getMaxParticipants(),
                entity.getAllowWaitlist(),
                entity.getIsActive(),
                java.util.List.of(),
                entity.getCreatedDate(),
                entity.getLastModifiedDate(),
                entity.getCreatedBy(),
                entity.getLastModifiedBy()
        ).withCategory(entity.getCategoryUuid())
                .withResourceLinks(entity.getVenueResourceUuid(), entity.getMarketplaceJobUuid());
    }

    public static ClassDefinition toEntity(ClassDefinitionDTO dto) {
        if (dto == null) {
            return null;
        }
        ClassDefinition entity = new ClassDefinition();
        entity.setUuid(dto.uuid());
        entity.setTitle(dto.title());
        entity.setDescription(dto.description());
        entity.setThumbnailUrl(FileUrlResolver.toStorableValue(dto.thumbnailUrl()));
        entity.setPromotionalVideoUrl(FileUrlResolver.toStorableValue(dto.promotionalVideoUrl()));
        entity.setDefaultInstructorUuid(dto.defaultInstructorUuid());
        entity.setOrganisationUuid(dto.organisationUuid());
        entity.setBranchUuid(dto.branchUuid());
        entity.setCourseUuid(dto.courseUuid());
        entity.setProgramUuid(dto.programUuid());
        entity.setCategoryUuid(dto.categoryUuid());
        entity.setSalePrice(dto.salePrice());
        entity.setInstructorPay(dto.instructorPay());
        entity.setRateBasis(dto.rateBasis() == null ? RateBasis.PER_HOUR : dto.rateBasis());
        entity.setClassVisibility(dto.classVisibility());
        entity.setSessionFormat(dto.sessionFormat());
        entity.setDefaultStartTime(dto.defaultStartTime());
        entity.setDefaultEndTime(dto.defaultEndTime());
        entity.setAcademicPeriodStartDate(dto.academicPeriodStartDate());
        entity.setAcademicPeriodEndDate(dto.academicPeriodEndDate());
        entity.setRegistrationPeriodStartDate(registrationOpensOn(dto));
        entity.setRegistrationPeriodEndDate(registrationClosesOn(dto));
        entity.setClassReminderMinutes(dto.classReminderMinutes());
        entity.setClassColor(dto.classColor());
        entity.setLocationType(dto.locationType());
        entity.setLocationName(dto.locationName());
        entity.setLocationLatitude(dto.locationLatitude());
        entity.setLocationLongitude(dto.locationLongitude());
        entity.setMeetingLink(dto.meetingLink());
        entity.setMaxParticipants(dto.maxParticipants());
        entity.setAllowWaitlist(dto.allowWaitlist());
        entity.setIsActive(dto.isActive());
        entity.setVenueResourceUuid(dto.venueResourceUuid());
        entity.setMarketplaceJobUuid(dto.marketplaceJobUuid());
        return entity;
    }

    /**
     * The registration window is mandatory in the schema and required of every API caller, but a
     * class can also be raised internally — a marketplace job that finds its instructor becomes a
     * class, and a job may carry no window of its own. Rather than fail that insert, fall back the
     * same way the backfill migration did: open at the academic period start, or the day the class
     * is raised.
     */
    private static LocalDate registrationOpensOn(ClassDefinitionDTO dto) {
        if (dto.registrationPeriodStartDate() != null) {
            return dto.registrationPeriodStartDate();
        }
        return dto.academicPeriodStartDate() != null
                ? dto.academicPeriodStartDate()
                : LocalDate.now(ZoneOffset.UTC);
    }

    /**
     * Closes at the academic period end, or at the far-future sentinel the migration used, so a
     * class raised without a stated window stays open rather than arriving already shut. Never
     * before the day it opens.
     */
    private static LocalDate registrationClosesOn(ClassDefinitionDTO dto) {
        if (dto.registrationPeriodEndDate() != null) {
            return dto.registrationPeriodEndDate();
        }
        LocalDate opensOn = registrationOpensOn(dto);
        LocalDate academicEnd = dto.academicPeriodEndDate();
        return academicEnd != null && !academicEnd.isBefore(opensOn) ? academicEnd : OPEN_ENDED_REGISTRATION;
    }

    public static void updateEntityFromDTO(ClassDefinition entity, ClassDefinitionDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        
        if (dto.title() != null) {
            entity.setTitle(dto.title());
        }
        if (dto.description() != null) {
            entity.setDescription(dto.description());
        }
        if (dto.thumbnailUrl() != null) {
            entity.setThumbnailUrl(FileUrlResolver.toStorableValue(dto.thumbnailUrl()));
        }
        if (dto.promotionalVideoUrl() != null) {
            entity.setPromotionalVideoUrl(FileUrlResolver.toStorableValue(dto.promotionalVideoUrl()));
        }
        if (dto.defaultInstructorUuid() != null) {
            entity.setDefaultInstructorUuid(dto.defaultInstructorUuid());
        }
        if (dto.organisationUuid() != null) {
            entity.setOrganisationUuid(dto.organisationUuid());
        }
        if (dto.branchUuid() != null) {
            entity.setBranchUuid(dto.branchUuid());
        }
        if (dto.courseUuid() != null) {
            entity.setCourseUuid(dto.courseUuid());
        }
        if (dto.programUuid() != null) {
            entity.setProgramUuid(dto.programUuid());
        }
        if (dto.categoryUuid() != null) {
            entity.setCategoryUuid(dto.categoryUuid());
        }
        if (dto.salePrice() != null) {
            entity.setSalePrice(dto.salePrice());
        }
        if (dto.rateBasis() != null) {
            entity.setRateBasis(dto.rateBasis());
        }
        if (dto.instructorPay() != null) {
            entity.setInstructorPay(dto.instructorPay());
        }
        if (dto.classVisibility() != null) {
            entity.setClassVisibility(dto.classVisibility());
        }
        if (dto.sessionFormat() != null) {
            entity.setSessionFormat(dto.sessionFormat());
        }
        if (dto.defaultStartTime() != null) {
            entity.setDefaultStartTime(dto.defaultStartTime());
        }
        if (dto.defaultEndTime() != null) {
            entity.setDefaultEndTime(dto.defaultEndTime());
        }
        if (dto.academicPeriodStartDate() != null) {
            entity.setAcademicPeriodStartDate(dto.academicPeriodStartDate());
        }
        if (dto.academicPeriodEndDate() != null) {
            entity.setAcademicPeriodEndDate(dto.academicPeriodEndDate());
        }
        if (dto.registrationPeriodStartDate() != null) {
            entity.setRegistrationPeriodStartDate(dto.registrationPeriodStartDate());
        }
        if (dto.registrationPeriodEndDate() != null) {
            entity.setRegistrationPeriodEndDate(dto.registrationPeriodEndDate());
        }
        if (dto.classReminderMinutes() != null) {
            entity.setClassReminderMinutes(dto.classReminderMinutes());
        }
        if (dto.classColor() != null) {
            entity.setClassColor(dto.classColor());
        }
        if (dto.locationType() != null) {
            entity.setLocationType(dto.locationType());
        }
        if (dto.locationName() != null) {
            entity.setLocationName(dto.locationName());
        }
        if (dto.locationLatitude() != null) {
            entity.setLocationLatitude(dto.locationLatitude());
        }
        if (dto.locationLongitude() != null) {
            entity.setLocationLongitude(dto.locationLongitude());
        }
        if (dto.meetingLink() != null) {
            entity.setMeetingLink(dto.meetingLink());
        }
        if (dto.maxParticipants() != null) {
            entity.setMaxParticipants(dto.maxParticipants());
        }
        if (dto.allowWaitlist() != null) {
            entity.setAllowWaitlist(dto.allowWaitlist());
        }
        if (dto.isActive() != null) {
            entity.setIsActive(dto.isActive());
        }
        if (dto.venueResourceUuid() != null) {
            entity.setVenueResourceUuid(dto.venueResourceUuid());
        }
    }
}
