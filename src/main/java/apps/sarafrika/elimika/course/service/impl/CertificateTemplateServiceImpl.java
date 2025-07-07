package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.GenericSpecificationBuilder;
import apps.sarafrika.elimika.course.dto.CertificateTemplateDTO;
import apps.sarafrika.elimika.course.factory.CertificateTemplateFactory;
import apps.sarafrika.elimika.course.model.CertificateTemplate;
import apps.sarafrika.elimika.course.repository.CertificateTemplateRepository;
import apps.sarafrika.elimika.course.service.CertificateTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CertificateTemplateServiceImpl implements CertificateTemplateService {

    private final CertificateTemplateRepository certificateTemplateRepository;
    private final GenericSpecificationBuilder<CertificateTemplate> specificationBuilder;

    private static final String CERTIFICATE_TEMPLATE_NOT_FOUND_TEMPLATE = "Certificate template with ID %s not found";

    @Override
    public CertificateTemplateDTO createCertificateTemplate(CertificateTemplateDTO certificateTemplateDTO) {
        CertificateTemplate certificateTemplate = CertificateTemplateFactory.toEntity(certificateTemplateDTO);

        if (certificateTemplate.getIsActive() == null) {
            certificateTemplate.setIsActive(false);
        }

        CertificateTemplate savedCertificateTemplate = certificateTemplateRepository.save(certificateTemplate);
        return CertificateTemplateFactory.toDTO(savedCertificateTemplate);
    }

    @Override
    @Transactional(readOnly = true)
    public CertificateTemplateDTO getCertificateTemplateByUuid(UUID uuid) {
        return certificateTemplateRepository.findByUuid(uuid)
                .map(CertificateTemplateFactory::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(CERTIFICATE_TEMPLATE_NOT_FOUND_TEMPLATE, uuid)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CertificateTemplateDTO> getAllCertificateTemplates(Pageable pageable) {
        return certificateTemplateRepository.findAll(pageable).map(CertificateTemplateFactory::toDTO);
    }

    @Override
    public CertificateTemplateDTO updateCertificateTemplate(UUID uuid, CertificateTemplateDTO certificateTemplateDTO) {
        CertificateTemplate existingCertificateTemplate = certificateTemplateRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(CERTIFICATE_TEMPLATE_NOT_FOUND_TEMPLATE, uuid)));

        updateCertificateTemplateFields(existingCertificateTemplate, certificateTemplateDTO);

        CertificateTemplate updatedCertificateTemplate = certificateTemplateRepository.save(existingCertificateTemplate);
        return CertificateTemplateFactory.toDTO(updatedCertificateTemplate);
    }

    @Override
    public void deleteCertificateTemplate(UUID uuid) {
        if (!certificateTemplateRepository.existsByUuid(uuid)) {
            throw new ResourceNotFoundException(
                    String.format(CERTIFICATE_TEMPLATE_NOT_FOUND_TEMPLATE, uuid));
        }
        certificateTemplateRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CertificateTemplateDTO> search(Map<String, String> searchParams, Pageable pageable) {
        Specification<CertificateTemplate> spec = specificationBuilder.buildSpecification(
                CertificateTemplate.class, searchParams);
        return certificateTemplateRepository.findAll(spec, pageable).map(CertificateTemplateFactory::toDTO);
    }

    private void updateCertificateTemplateFields(CertificateTemplate existingCertificateTemplate, CertificateTemplateDTO dto) {
        if (dto.name() != null) {
            existingCertificateTemplate.setName(dto.name());
        }
        if (dto.templateType() != null) {
            existingCertificateTemplate.setTemplateType(dto.templateType());
        }
        if (dto.templateHtml() != null) {
            existingCertificateTemplate.setTemplateHtml(dto.templateHtml());
        }
        if (dto.templateCss() != null) {
            existingCertificateTemplate.setTemplateCss(dto.templateCss());
        }
        if (dto.backgroundImageUrl() != null) {
            existingCertificateTemplate.setBackgroundImageUrl(dto.backgroundImageUrl());
        }
        if (dto.active() != null) {
            existingCertificateTemplate.setIsActive(dto.active());
        }
    }
}