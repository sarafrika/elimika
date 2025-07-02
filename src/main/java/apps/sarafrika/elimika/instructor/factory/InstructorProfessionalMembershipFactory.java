package apps.sarafrika.elimika.instructor.factory;

import apps.sarafrika.elimika.instructor.dto.InstructorProfessionalMembershipDTO;
import apps.sarafrika.elimika.instructor.model.InstructorProfessionalMembership;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InstructorProfessionalMembershipFactory {

    // Convert InstructorProfessionalMembership entity to InstructorProfessionalMembershipDTO
    public static InstructorProfessionalMembershipDTO toDTO(InstructorProfessionalMembership membership) {
        if (membership == null) {
            return null;
        }
        return new InstructorProfessionalMembershipDTO(
                membership.getUuid(),
                membership.getInstructorUuid(),
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

    // Convert InstructorProfessionalMembershipDTO to InstructorProfessionalMembership entity
    public static InstructorProfessionalMembership toEntity(InstructorProfessionalMembershipDTO dto) {
        if (dto == null) {
            return null;
        }
        InstructorProfessionalMembership membership = new InstructorProfessionalMembership();
        membership.setUuid(dto.uuid());
        membership.setInstructorUuid(dto.instructorUuid());
        membership.setOrganizationName(dto.organizationName());
        membership.setMembershipNumber(dto.membershipNumber());
        membership.setStartDate(dto.startDate());
        membership.setEndDate(dto.endDate());
        membership.setIsActive(dto.isActive());
        return membership;
    }
}