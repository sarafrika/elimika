package apps.sarafrika.elimika.shared.otp;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface OneTimeCodeRepository extends JpaRepository<OneTimeCode, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from OneTimeCode c where c.purpose = :purpose and c.subjectUuid = :subject "
            + "and c.consumedAt is null and c.invalidatedAt is null order by c.id desc")
    List<OneTimeCode> findOpenForUpdate(@Param("purpose") OneTimeCodePurpose purpose, @Param("subject") UUID subject);

    @Query("select count(c) from OneTimeCode c where c.purpose = :purpose and c.subjectUuid = :subject "
            + "and c.createdDate >= :since")
    long countIssuedSince(@Param("purpose") OneTimeCodePurpose purpose, @Param("subject") UUID subject,
                          @Param("since") java.time.LocalDateTime since);

    Optional<OneTimeCode> findFirstByPurposeAndSubjectUuidOrderByIdDesc(OneTimeCodePurpose purpose, UUID subjectUuid);
}
