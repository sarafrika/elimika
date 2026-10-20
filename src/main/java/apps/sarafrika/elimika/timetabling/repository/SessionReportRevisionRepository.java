package apps.sarafrika.elimika.timetabling.repository;

import apps.sarafrika.elimika.timetabling.model.SessionReportRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionReportRevisionRepository extends JpaRepository<SessionReportRevision, Long> {

    Optional<SessionReportRevision> findFirstBySessionReportUuidOrderByRevisionNumberDesc(UUID sessionReportUuid);

    List<SessionReportRevision> findBySessionReportUuidOrderByRevisionNumberAsc(UUID sessionReportUuid);
}
