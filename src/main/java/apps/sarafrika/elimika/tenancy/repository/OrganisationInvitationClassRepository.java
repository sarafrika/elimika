package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.OrganisationInvitationClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Repository for the classes named in an invitation.
 *
 * @author Wilfred Njuguna
 * @since 1.0
 */
@Repository
public interface OrganisationInvitationClassRepository extends JpaRepository<OrganisationInvitationClass, Long> {

    List<OrganisationInvitationClass> findByInvitationUuid(UUID invitationUuid);

    List<OrganisationInvitationClass> findByInvitationUuidIn(Collection<UUID> invitationUuids);

    void deleteByInvitationUuid(UUID invitationUuid);
}
