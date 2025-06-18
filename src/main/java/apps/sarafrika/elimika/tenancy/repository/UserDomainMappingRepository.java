package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.UserDomainMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserDomainMappingRepository extends JpaRepository<UserDomainMapping, Long> {
    List<UserDomainMapping> findByUserUuid(UUID useruuid);

    boolean existsByUserUuidAndUserDomainUuid(UUID useruuid, UUID domainuuid);
}
