package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long>, JpaSpecificationExecutor<Certificate> {
    Optional<Certificate> findByUuid(UUID uuid);

    Optional<Certificate> findByCertificateNumber(String certificateNumber);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    boolean existsByStudentUuidAndCourseUuid(UUID studentUuid, UUID courseUuid);

    boolean existsByStudentUuidAndProgramUuid(UUID studentUuid, UUID programUuid);

    boolean existsByCertificateNumberAndIsValidTrue(String certificateNumber);

    List<Certificate> findByStudentUuid(UUID studentUuid);

    List<Certificate> findByStudentUuidAndIsValidTrue(UUID studentUuid);

    List<Certificate> findByCourseUuidIsNotNull();

    List<Certificate> findByProgramUuidIsNotNull();

    List<Certificate> findByIsValidFalse();
}