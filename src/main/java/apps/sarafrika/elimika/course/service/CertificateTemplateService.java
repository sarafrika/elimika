package apps.sarafrika.elimika.course.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface CertificateTemplateService {
    CertificateTemplateDTO createCertificateTemplate(CertificateTemplateDTO certificateTemplateDTO);

    CertificateTemplateDTO getCertificateTemplateByUuid(UUID uuid);

    Page<CertificateTemplateDTO> getAllCertificateTemplates(Pageable pageable);

    CertificateTemplateDTO updateCertificateTemplate(UUID uuid, CertificateTemplateDTO certificateTemplateDTO);

    void deleteCertificateTemplate(UUID uuid);

    Page<CertificateTemplateDTO> search(Map<String, String> searchParams, Pageable pageable);
}