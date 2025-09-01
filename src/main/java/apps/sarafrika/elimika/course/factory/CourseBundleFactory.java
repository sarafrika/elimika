package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CourseBundleDTO;
import apps.sarafrika.elimika.course.model.CourseBundle;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseBundleFactory {

    /**
     * Convert CourseBundle entity to CourseBundleDTO.
     *
     * @param courseBundle the course bundle entity
     * @return course bundle DTO
     */
    public static CourseBundleDTO toDTO(CourseBundle courseBundle) {
        if (courseBundle == null) {
            return null;
        }
        
        return new CourseBundleDTO(
                courseBundle.getUuid(),
                courseBundle.getName(),
                courseBundle.getInstructorUuid(),
                courseBundle.getDescription(),
                courseBundle.getPrice(),
                courseBundle.getStatus(),
                courseBundle.getActive(),
                courseBundle.getValidityDays(),
                courseBundle.getDiscountPercentage(),
                courseBundle.getThumbnailUrl(),
                courseBundle.getBannerUrl(),
                courseBundle.getCreatedDate(),
                courseBundle.getCreatedBy(),
                courseBundle.getLastModifiedDate(),
                courseBundle.getLastModifiedBy()
        );
    }

    /**
     * Convert CourseBundleDTO to CourseBundle entity.
     *
     * @param dto the course bundle DTO
     * @return course bundle entity
     */
    public static CourseBundle toEntity(CourseBundleDTO dto) {
        if (dto == null) {
            return null;
        }

        CourseBundle courseBundle = new CourseBundle();
        courseBundle.setUuid(dto.uuid());
        courseBundle.setName(dto.name());
        courseBundle.setInstructorUuid(dto.instructorUuid());
        courseBundle.setDescription(dto.description());
        courseBundle.setPrice(dto.price());
        courseBundle.setStatus(dto.status());
        courseBundle.setActive(dto.active());
        courseBundle.setValidityDays(dto.validityDays());
        courseBundle.setDiscountPercentage(dto.discountPercentage());
        courseBundle.setThumbnailUrl(dto.thumbnailUrl());
        courseBundle.setBannerUrl(dto.bannerUrl());
        courseBundle.setCreatedDate(dto.createdDate());
        courseBundle.setCreatedBy(dto.createdBy());
        courseBundle.setLastModifiedDate(dto.updatedDate());
        courseBundle.setLastModifiedBy(dto.updatedBy());

        return courseBundle;
    }

    /**
     * Update existing CourseBundle entity with data from DTO.
     *
     * @param existingBundle the existing course bundle entity
     * @param dto the course bundle DTO with updated data
     */
    public static void updateEntityFromDTO(CourseBundle existingBundle, CourseBundleDTO dto) {
        if (existingBundle == null || dto == null) {
            return;
        }

        if (dto.name() != null) {
            existingBundle.setName(dto.name());
        }
        if (dto.instructorUuid() != null) {
            existingBundle.setInstructorUuid(dto.instructorUuid());
        }
        if (dto.description() != null) {
            existingBundle.setDescription(dto.description());
        }
        if (dto.price() != null) {
            existingBundle.setPrice(dto.price());
        }
        if (dto.status() != null) {
            existingBundle.setStatus(dto.status());
        }
        if (dto.active() != null) {
            existingBundle.setActive(dto.active());
        }
        if (dto.validityDays() != null) {
            existingBundle.setValidityDays(dto.validityDays());
        }
        if (dto.discountPercentage() != null) {
            existingBundle.setDiscountPercentage(dto.discountPercentage());
        }
        if (dto.thumbnailUrl() != null) {
            existingBundle.setThumbnailUrl(dto.thumbnailUrl());
        }
        if (dto.bannerUrl() != null) {
            existingBundle.setBannerUrl(dto.bannerUrl());
        }
    }
}