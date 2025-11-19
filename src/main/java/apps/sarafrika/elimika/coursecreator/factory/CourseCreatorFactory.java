package apps.sarafrika.elimika.coursecreator.factory;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorDTO;
import apps.sarafrika.elimika.coursecreator.model.CourseCreator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseCreatorFactory {

    // Convert CourseCreator entity to CourseCreatorDTO
    public static CourseCreatorDTO toDTO(CourseCreator courseCreator) {
        if (courseCreator == null) {
            return null;
        }
        return new CourseCreatorDTO(
                courseCreator.getUuid(),
                courseCreator.getUserUuid(),
                courseCreator.getFullName(),
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
