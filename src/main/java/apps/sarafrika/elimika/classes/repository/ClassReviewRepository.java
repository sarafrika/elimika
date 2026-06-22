package apps.sarafrika.elimika.classes.repository;

import apps.sarafrika.elimika.classes.model.ClassReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassReviewRepository extends JpaRepository<ClassReview, Long> {

    Page<ClassReview> findByClassDefinitionUuid(UUID classDefinitionUuid, Pageable pageable);

    Optional<ClassReview> findByClassDefinitionUuidAndStudentUuid(UUID classDefinitionUuid, UUID studentUuid);

    long countByClassDefinitionUuid(UUID classDefinitionUuid);

    @Query("SELECT AVG(r.rating) FROM ClassReview r WHERE r.classDefinitionUuid = :classDefinitionUuid")
    Double findAverageRatingForClassDefinition(@Param("classDefinitionUuid") UUID classDefinitionUuid);
}
