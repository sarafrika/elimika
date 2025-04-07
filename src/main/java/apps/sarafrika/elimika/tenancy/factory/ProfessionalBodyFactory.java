package apps.sarafrika.elimika.tenancy.factory;

import apps.sarafrika.elimika.tenancy.dto.ProfessionalBodyDTO;
import apps.sarafrika.elimika.tenancy.entity.ProfessionalBody;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProfessionalBodyFactory {

    public static ProfessionalBodyDTO toDTO(ProfessionalBody professionalBody) {
        return new ProfessionalBodyDTO(
                professionalBody.getBodyName(),
                professionalBody.getMembershipNo(),
                professionalBody.getMemberSince(),
                professionalBody.getUserUuid()
        );
    }

    public static ProfessionalBody toEntity(ProfessionalBodyDTO dto) {
        ProfessionalBody professionalBody = new ProfessionalBody();
        professionalBody.setBodyName(dto.bodyName());
        professionalBody.setMembershipNo(dto.membershipNo());
        professionalBody.setMemberSince(dto.memberSince());
        professionalBody.setUserUuid(dto.userUuid());
        return professionalBody;
    }
}
