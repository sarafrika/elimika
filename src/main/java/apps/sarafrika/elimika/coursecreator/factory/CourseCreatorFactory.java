package apps.sarafrika.elimika.coursecreator.factory;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorDTO;
import apps.sarafrika.elimika.coursecreator.model.CourseCreator;
import apps.sarafrika.elimika.shared.utils.CoordinatePrecision;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseCreatorFactory {

    // Convert CourseCreator entity to CourseCreatorDTO
    public static CourseCreatorDTO toDTO(CourseCreator courseCreator) {
        if (courseCreator == null) {
            return null;
        }
        return toDTO(courseCreator, courseCreator.getLatitude(), courseCreator.getLongitude());
    }

    /**
     * The directory form of a profile: identical to {@link #toDTO(CourseCreator)} except that latitude and
     * longitude are rounded to town level (see {@link CoordinatePrecision}). Used for every list and
     * search response, and for single-profile reads by anyone other than the owner or a platform
     * admin.
     */
    public static CourseCreatorDTO toPublicDTO(CourseCreator courseCreator) {
        if (courseCreator == null) {
            return null;
        }
        return toDTO(courseCreator,
                CoordinatePrecision.toPublic(courseCreator.getLatitude()),
                CoordinatePrecision.toPublic(courseCreator.getLongitude()));
    }

    private static CourseCreatorDTO toDTO(CourseCreator courseCreator, BigDecimal latitude, BigDecimal longitude) {
        return new CourseCreatorDTO(
                courseCreator.getUuid(),
                courseCreator.getUserUuid(),
                courseCreator.getFullName(),
                courseCreator.getLocationName(),
                latitude,
                longitude,
                courseCreator.getBio(),
                courseCreator.getProfessionalHeadline(),
                courseCreator.getWebsite(),
                courseCreator.getAdminVerified(),
                courseCreator.getCreatedDate(),
                courseCreator.getCreatedBy(),
                courseCreator.getLastModifiedDate(),
                courseCreator.getLastModifiedBy()
        );
    }

    // Convert CourseCreatorDTO to CourseCreator entity
    public static CourseCreator toEntity(CourseCreatorDTO dto) {
        if (dto == null) {
            return null;
        }
        CourseCreator courseCreator = new CourseCreator();
        courseCreator.setUuid(dto.uuid());
        courseCreator.setUserUuid(dto.userUuid());
        courseCreator.setFullName(dto.fullName());
        courseCreator.setLocationName(dto.locationName());
        courseCreator.setLatitude(dto.latitude());
        courseCreator.setLongitude(dto.longitude());
        courseCreator.setBio(dto.bio());
        courseCreator.setProfessionalHeadline(dto.professionalHeadline());
        courseCreator.setWebsite(dto.website());
        courseCreator.setAdminVerified(dto.adminVerified());
        courseCreator.setCreatedDate(dto.createdDate());
        courseCreator.setCreatedBy(dto.createdBy());
        courseCreator.setLastModifiedDate(dto.updatedDate());
        courseCreator.setLastModifiedBy(dto.updatedBy());
        return courseCreator;
    }
}
