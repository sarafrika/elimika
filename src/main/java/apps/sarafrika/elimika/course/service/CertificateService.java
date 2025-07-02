package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CertificateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface CertificateService {
    CertificateDTO createCertificate(CertificateDTO certificateDTO);

    CertificateDTO getCertificateByUuid(UUID uuid);

    Page<CertificateDTO> getAllCertificates(Pageable pageable);

    CertificateDTO updateCertificate(UUID uuid, CertificateDTO certificateDTO);

    void deleteCertificate(UUID uuid);

    Page<CertificateDTO> search(Map<String, String> searchParams, Pageable pageable);
}