package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CertificateTemplateDTO;
import apps.sarafrika.elimika.course.model.CertificateTemplate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CertificateTemplateFactory {

    // Convert CertificateTemplate entity to CertificateTemplateDTO
    public static CertificateTemplateDTO toDTO(CertificateTemplate certificateTemplate) {
        if (certificateTemplate == null) {
            return null;
        }
        return new CertificateTemplateDTO(
                certificateTemplate.getUuid(),
                certificateTemplate.getName(),
                certificateTemplate.getTemplateType(),
                certificateTemplate.getTemplateHtml(),
                certificateTemplate.getTemplateCss(),
                certificateTemplate.getBackgroundImageUrl(),
                certificateTemplate.getActive(),
                certificateTemplate.getCreatedDate(),
                certificateTemplate.getCreatedBy(),
                certificateTemplate.getLastModifiedDate(),
                certificateTemplate.getLastModifiedBy()
        );
    }

    // Convert CertificateTemplateDTO to CertificateTemplate entity
    public static CertificateTemplate toEntity(CertificateTemplateDTO dto) {
        if (dto == null) {
            return null;
        }
        CertificateTemplate certificateTemplate = new CertificateTemplate();
        certificateTemplate.setUuid(dto.uuid());
        certificateTemplate.setName(dto.name());
        certificateTemplate.setTemplateType(dto.templateType());
        certificateTemplate.setTemplateHtml(dto.templateHtml());
        certificateTemplate.setTemplateCss(dto.templateCss());
        certificateTemplate.setBackgroundImageUrl(dto.backgroundImageUrl());
        certificateTemplate.setActive(dto.active());
        certificateTemplate.setCreatedDate(dto.createdDate());
        certificateTemplate.setCreatedBy(dto.createdBy());
        certificateTemplate.setLastModifiedDate(dto.updatedDate());
        certificateTemplate.setLastModifiedBy(dto.updatedBy());
        return certificateTemplate;
    }
}
