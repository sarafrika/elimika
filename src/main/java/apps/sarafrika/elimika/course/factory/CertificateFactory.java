package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CertificateDTO;
import apps.sarafrika.elimika.course.model.Certificate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CertificateFactory {

    // Convert Certificate entity to CertificateDTO
    public static CertificateDTO toDTO(Certificate certificate) {
        if (certificate == null) {
            return null;
        }
        return new CertificateDTO(
                certificate.getUuid(),
                certificate.getCertificateNumber(),
                certificate.getStudentUuid(),
                certificate.getCourseUuid(),
                certificate.getProgramUuid(),
                certificate.getTemplateUuid(),
                certificate.getIssuedDate(),
                certificate.getCompletionDate(),
                certificate.getFinalGrade(),
                certificate.getCertificateUrl(),
                certificate.getIsValid(),
                certificate.getRevokedAt(),
                certificate.getRevokedReason(),
                certificate.getCreatedDate(),
                certificate.getCreatedBy(),
                certificate.getLastModifiedDate(),
                certificate.getLastModifiedBy()
        );
    }

    // Convert CertificateDTO to Certificate entity
    public static Certificate toEntity(CertificateDTO dto) {
        if (dto == null) {
            return null;
        }
        Certificate certificate = new Certificate();
        certificate.setUuid(dto.uuid());
        certificate.setCertificateNumber(dto.certificateNumber());
        certificate.setStudentUuid(dto.studentUuid());
        certificate.setCourseUuid(dto.courseUuid());
        certificate.setProgramUuid(dto.programUuid());
        certificate.setTemplateUuid(dto.templateUuid());
        certificate.setIssuedDate(dto.issuedDate());
        certificate.setCompletionDate(dto.completionDate());
        certificate.setFinalGrade(dto.finalGrade());
        certificate.setCertificateUrl(dto.certificateUrl());
        certificate.setIsValid(dto.isValid());
        certificate.setRevokedAt(dto.revokedAt());
        certificate.setRevokedReason(dto.revokedReason());
        certificate.setCreatedDate(dto.createdDate());
        certificate.setCreatedBy(dto.createdBy());
        certificate.setLastModifiedDate(dto.updatedDate());
        certificate.setLastModifiedBy(dto.updatedBy());
        return certificate;
    }
}