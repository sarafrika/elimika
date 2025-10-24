package apps.sarafrika.elimika.instructor.repository;

import apps.sarafrika.elimika.shared.utils.enums.DocumentStatus;
import apps.sarafrika.elimika.instructor.model.InstructorDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstructorDocumentRepository extends JpaRepository<InstructorDocument,Long>,
        JpaSpecificationExecutor<InstructorDocument> {
    Optional<InstructorDocument> findByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    Optional<InstructorDocument> findByInstructorUuid(UUID instructorUuid);

    List<InstructorDocument> findByDocumentTypeUuid(UUID documentTypeUuid);

    List<InstructorDocument> findByInstructorUuidAndDocumentTypeUuid(UUID instructorUuid, UUID documentTypeUuid);

    List<InstructorDocument> findByExpiryDateBeforeAndStatusNot(LocalDate cutoffDate, DocumentStatus status);

    List<InstructorDocument> findByIsVerifiedFalse();

    long countByIsVerifiedFalse();

    @Query("SELECT COUNT(id) FROM InstructorDocument id WHERE id.expiryDate BETWEEN :start AND :end " +
            "AND (id.status IS NULL OR id.status <> :excludedStatus)")
    long countExpiringBetweenExcludingStatus(@Param("start") LocalDate start,
                                             @Param("end") LocalDate end,
                                             @Param("excludedStatus") DocumentStatus excludedStatus);
}
