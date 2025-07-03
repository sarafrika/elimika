package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CourseDTO;
import apps.sarafrika.elimika.course.model.Course;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseFactory {

    // Convert Course entity to CourseDTO
    public static CourseDTO toDTO(Course course) {
        if (course == null) {
            return null;
        }
        return new CourseDTO(
                course.getUuid(),
                course.getName(),
                course.getInstructorUuid(),
                course.getCategoryUuid(),
                course.getDifficultyUuid(),
                course.getDescription(),
                course.getObjectives(),
                course.getPrerequisites(),
                course.getDurationHours(),
                course.getDurationMinutes(),
                course.getClassLimit(),
                course.getPrice(),
                course.getThumbnailUrl(),
                course.getIntroVideoUrl(),
                course.getBannerUrl(),
                course.getStatus(),
                course.getActive(),
                course.getCreatedDate(),
                course.getCreatedBy(),
                course.getLastModifiedDate(),
                course.getLastModifiedBy()
        );
    }

    // Convert CourseDTO to Course entity
    public static Course toEntity(CourseDTO dto) {
        if (dto == null) {
            return null;
        }
        Course course = new Course();
        course.setUuid(dto.uuid());
        course.setName(dto.name());
        course.setInstructorUuid(dto.instructorUuid());
        course.setCategoryUuid(dto.categoryUuid());
        course.setDifficultyUuid(dto.difficultyUuid());
        course.setDescription(dto.description());
        course.setObjectives(dto.objectives());
        course.setPrerequisites(dto.prerequisites());
        course.setDurationHours(dto.durationHours());
        course.setDurationMinutes(dto.durationMinutes());
        course.setClassLimit(dto.classLimit());
        course.setPrice(dto.price());
        course.setThumbnailUrl(dto.thumbnailUrl());
        course.setIntroVideoUrl(dto.introVideoUrl());
        course.setBannerUrl(dto.bannerUrl());
        course.setStatus(dto.status());
        course.setActive(dto.active());
        course.setCreatedDate(dto.createdDate());
        course.setCreatedBy(dto.createdBy());
        course.setLastModifiedDate(dto.updatedDate());
        course.setLastModifiedBy(dto.updatedBy());
        return course;
    }
}