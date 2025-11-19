package apps.sarafrika.elimika.coursecreator.factory;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorCertificationDTO;
import apps.sarafrika.elimika.coursecreator.model.CourseCreatorCertification;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CourseCreatorCertificationFactory {

    public static CourseCreatorCertificationDTO toDTO(CourseCreatorCertification certification) {
        if (certification == null) {
            return null;
        }
        return new CourseCreatorCertificationDTO(
                certification.getUuid(),
                certification.getCourseCreatorUuid(),
                certification.getCertificationName(),
                certification.getIssuingOrganization(),
                certification.getIssuedDate(),
                certification.getExpiryDate(),
                certification.getCredentialId(),
                certification.getCredentialUrl(),
                certification.getDescription(),
                certification.getIsVerified(),
                certification.getCreatedDate(),
                certification.getCreatedBy(),
                certification.getLastModifiedDate(),
                certification.getLastModifiedBy()
        );
    }

    public static CourseCreatorCertification toEntity(CourseCreatorCertificationDTO dto) {
        if (dto == null) {
            return null;
        }
        CourseCreatorCertification certification = new CourseCreatorCertification();
        certification.setUuid(dto.uuid());
        certification.setCourseCreatorUuid(dto.courseCreatorUuid());
        certification.setCertificationName(dto.certificationName());
        certification.setIssuingOrganization(dto.issuingOrganization());
        certification.setIssuedDate(dto.issuedDate());
        certification.setExpiryDate(dto.expiryDate());
        certification.setCredentialId(dto.credentialId());
        certification.setCredentialUrl(dto.credentialUrl());
        certification.setDescription(dto.description());
        certification.setIsVerified(dto.isVerified());
        certification.setCreatedDate(dto.createdDate());
        certification.setCreatedBy(dto.createdBy());
        certification.setLastModifiedDate(dto.updatedDate());
        certification.setLastModifiedBy(dto.updatedBy());
        return certification;
    }
}
