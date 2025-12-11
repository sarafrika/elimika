package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.UserDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface UserDomainRepository extends JpaRepository<UserDomain, Long> {
    Optional<UserDomain> findByDomainName(String domainName);
    Optional<UserDomain> findByUuid(UUID uuid);
    List<UserDomain> findByOrgSupportedTrue();
    Optional<UserDomain> findByDomainNameAndOrgSupportedTrue(String domainName);
    Optional<UserDomain> findByUuidAndOrgSupportedTrue(UUID uuid);
}
