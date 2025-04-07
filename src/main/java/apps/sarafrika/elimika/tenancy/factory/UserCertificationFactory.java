package apps.sarafrika.elimika.tenancy.factory;

import apps.sarafrika.elimika.tenancy.dto.UserCertificationDTO;
import apps.sarafrika.elimika.tenancy.entity.UserCertification;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserCertificationFactory {

    public static UserCertificationDTO toDTO(UserCertification userCertification) {
        return new UserCertificationDTO(
                userCertification.getIssuedDate(),
                userCertification.getIssuedBy(),
                userCertification.getCertificateUrl(),
                userCertification.getUserUuid()
        );
    }

    public static UserCertification toEntity(UserCertificationDTO dto) {
        UserCertification userCertification = new UserCertification();
        userCertification.setIssuedDate(dto.issuedDate());
        userCertification.setIssuedBy(dto.issuedBy());
        userCertification.setCertificateUrl(dto.certificateUrl());
        userCertification.setUserUuid(dto.userUuid());
        return userCertification;
    }
}
