package apps.sarafrika.elimika.student.repository;

import apps.sarafrika.elimika.student.model.StudentGuardianLink;
import apps.sarafrika.elimika.student.util.enums.GuardianLinkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentGuardianLinkRepository extends JpaRepository<StudentGuardianLink, Long> {

    Optional<StudentGuardianLink> findByUuid(UUID uuid);

    boolean existsByStudentUuidAndGuardianUserUuidAndStatusIn(UUID studentUuid,
                                                              UUID guardianUuid,
                                                              Collection<GuardianLinkStatus> statuses);

    boolean existsByStudentUuidAndGuardianUserUuidAndStatus(UUID studentUuid,
                                                            UUID guardianUuid,
                                                            GuardianLinkStatus status);

    List<StudentGuardianLink> findByGuardianUserUuidAndStatus(UUID guardianUuid, GuardianLinkStatus status);

    List<StudentGuardianLink> findByGuardianUserUuidAndStatusIn(UUID guardianUuid, Collection<GuardianLinkStatus> statuses);

    List<StudentGuardianLink> findByStudentUuidAndStatus(UUID studentUuid, GuardianLinkStatus status);

    Optional<StudentGuardianLink> findByStudentUuidAndGuardianUserUuidAndStatusIn(UUID studentUuid,
                                                                                  UUID guardianUuid,
                                                                                  Collection<GuardianLinkStatus> statuses);
}
