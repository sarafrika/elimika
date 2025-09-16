package apps.sarafrika.elimika.instructor.repository;

import apps.sarafrika.elimika.instructor.model.Instructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface InstructorRepository extends JpaRepository<Instructor, Long>, JpaSpecificationExecutor<Instructor> {

    Set<Instructor> findByIdIn(Set<Long> ids);

    Optional<Instructor> findByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    boolean existsByUuid(UUID uuid);

    boolean existsByUserUuid(UUID userUuid);

    /**
     * Find instructors by their verification status with pagination.
     *
     * @param adminVerified the verification status to filter by
     * @param pageable pagination information
     * @return paginated list of instructors with the specified verification status
     */
    Page<Instructor> findByAdminVerified(Boolean adminVerified, Pageable pageable);

    /**
     * Count instructors by their verification status.
     *
     * @param adminVerified the verification status to count
     * @return number of instructors with the specified verification status
     */
    long countByAdminVerified(Boolean adminVerified);
}
