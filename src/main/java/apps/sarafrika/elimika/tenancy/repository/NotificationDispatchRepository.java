package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.NotificationDispatch;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationDispatchRepository extends JpaRepository<NotificationDispatch, Long> {

    /** An organisation's outgoing broadcasts, newest first. */
    List<NotificationDispatch> findByOrganisationUuidOrderByCreatedDateDesc(
            UUID organisationUuid, Pageable pageable);
}
