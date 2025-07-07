package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentTypeRepository extends JpaRepository<ContentType, Long>, JpaSpecificationExecutor<ContentType> {
    Optional<ContentType> findByUuid(UUID uuid);

    Optional<ContentType> findByName(String name);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    @Query("SELECT ct FROM ContentType ct WHERE :mimeType MEMBER OF ct.mimeTypes")
    List<ContentType> findByMimeTypesContaining(@Param("mimeType") String mimeType);

    List<ContentType> findByMaxFileSizeMbIsNull();

    List<ContentType> findByMaxFileSizeMbGreaterThan(int maxFileSizeMb);

    @Query("SELECT CASE WHEN COUNT(ct) > 0 THEN true ELSE false END FROM ContentType ct WHERE :mimeType MEMBER OF ct.mimeTypes")
    boolean existsByMimeTypesContaining(@Param("mimeType") String mimeType);
}