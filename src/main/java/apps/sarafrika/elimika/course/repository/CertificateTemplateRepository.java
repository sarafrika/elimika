package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CertificateTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CertificateTemplateRepository extends JpaRepository<CertificateTemplate, Long>, JpaSpecificationExecutor<CertificateTemplate> {
    Optional<CertificateTemplate> findByUuid(UUID uuid);

    Optional<CertificateTemplate> findByName(String name);

    void deleteByUuid(UUID uuid);
}