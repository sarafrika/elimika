package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.ProgramEnrollmentRemark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProgramEnrollmentRemarkRepository extends JpaRepository<ProgramEnrollmentRemark, Long> {

    List<ProgramEnrollmentRemark> findByProgramEnrollmentUuidOrderByCreatedDateDescIdDesc(UUID programEnrollmentUuid);
}
