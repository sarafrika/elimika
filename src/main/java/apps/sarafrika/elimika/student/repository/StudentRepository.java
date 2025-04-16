package apps.sarafrika.elimika.student.repository;

import apps.sarafrika.elimika.student.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {
    boolean existsByUuid(UUID uuid);

    void deleteByUuid(UUID uuid);

    Optional<Student> findByUuid(UUID uuid);

    boolean existsByUserUuid(UUID userUuid);
}
