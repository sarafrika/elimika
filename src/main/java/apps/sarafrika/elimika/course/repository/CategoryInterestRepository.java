package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.CategoryInterest;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CategoryInterestRepository extends JpaRepository<CategoryInterest, Long> {

    List<CategoryInterest> findByUserUuidAndUserDomainOrderByIdAsc(UUID userUuid, UserDomain userDomain);

    /** One statement, so a row another request already removed is not an error. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM CategoryInterest i WHERE i.userUuid = :userUuid AND i.userDomain = :userDomain")
    void deleteByUserUuidAndUserDomain(@Param("userUuid") UUID userUuid, @Param("userDomain") UserDomain userDomain);
}
