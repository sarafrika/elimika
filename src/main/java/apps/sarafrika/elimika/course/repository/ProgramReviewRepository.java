package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.ProgramReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProgramReviewRepository extends JpaRepository<ProgramReview, Long> {

    Page<ProgramReview> findByProgramUuid(UUID programUuid, Pageable pageable);

    Optional<ProgramReview> findByProgramUuidAndStudentUuid(UUID programUuid, UUID studentUuid);

    long countByProgramUuid(UUID programUuid);

    @Query("SELECT AVG(r.rating) FROM ProgramReview r WHERE r.programUuid = :programUuid")
    Double findAverageRatingForProgram(@Param("programUuid") UUID programUuid);
}
