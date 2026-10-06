package apps.sarafrika.elimika.student.repository;

import apps.sarafrika.elimika.student.model.StudentGuardianContact;
import apps.sarafrika.elimika.student.util.enums.GuardianContactStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentGuardianContactRepository extends JpaRepository<StudentGuardianContact, Long> {

    Optional<StudentGuardianContact> findByUuid(UUID uuid);

    Optional<StudentGuardianContact> findByTokenHash(String tokenHash);

    List<StudentGuardianContact> findByStudentUuidAndStatusNotOrderByPositionAsc(UUID studentUuid,
                                                                                 GuardianContactStatus status);

    List<StudentGuardianContact> findByGuardianEmailAndStatusIn(String guardianEmail,
                                                                Collection<GuardianContactStatus> statuses);
}
