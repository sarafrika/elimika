package apps.sarafrika.elimika.coursecreator.factory;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorProfessionalMembershipDTO;
import apps.sarafrika.elimika.coursecreator.model.CourseCreatorProfessionalMembership;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CourseCreatorProfessionalMembershipFactory {

    public static CourseCreatorProfessionalMembershipDTO toDTO(CourseCreatorProfessionalMembership membership) {
        if (membership == null) {
            return null;
        }
        return new CourseCreatorProfessionalMembershipDTO(
                membership.getUuid(),
                membership.getCourseCreatorUuid(),
                membership.getOrganizationName(),
                membership.getMembershipNumber(),
                membership.getStartDate(),
                membership.getEndDate(),
                membership.getIsActive(),
                membership.getCreatedDate(),
                membership.getCreatedBy(),
                membership.getLastModifiedDate(),
                membership.getLastModifiedBy()
        );
    }

    public static CourseCreatorProfessionalMembership toEntity(CourseCreatorProfessionalMembershipDTO dto) {
        if (dto == null) {
            return null;
        }
        CourseCreatorProfessionalMembership membership = new CourseCreatorProfessionalMembership();
        membership.setUuid(dto.uuid());
        membership.setCourseCreatorUuid(dto.courseCreatorUuid());
        membership.setOrganizationName(dto.organizationName());
        membership.setMembershipNumber(dto.membershipNumber());
        membership.setStartDate(dto.startDate());
        membership.setEndDate(dto.endDate());
        membership.setIsActive(dto.isActive());
        membership.setCreatedDate(dto.createdDate());
        membership.setCreatedBy(dto.createdBy());
        membership.setLastModifiedDate(dto.updatedDate());
        membership.setLastModifiedBy(dto.updatedBy());
        return membership;
    }
}
