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

    @Query(value = """
            SELECT * FROM lesson_content_types 
            WHERE :mimeType = ANY(mime_types)
            """, nativeQuery = true)
    List<ContentType> findByMimeTypesContaining(@Param("mimeType") String mimeType);

    List<ContentType> findByMaxFileSizeMbIsNull();

    List<ContentType> findByMaxFileSizeMbGreaterThan(int maxFileSizeMb);

    @Query(value = """
            SELECT EXISTS(
                SELECT 1 FROM lesson_content_types 
                WHERE :mimeType = ANY(mime_types)
            )
            """, nativeQuery = true)
    boolean existsByMimeTypesContaining(@Param("mimeType") String mimeType);
}