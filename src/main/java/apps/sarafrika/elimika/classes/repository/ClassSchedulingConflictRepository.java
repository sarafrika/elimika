package apps.sarafrika.elimika.classes.repository;

import apps.sarafrika.elimika.classes.model.ClassSchedulingConflict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClassSchedulingConflictRepository extends JpaRepository<ClassSchedulingConflict, Long> {

    List<ClassSchedulingConflict> findByClassDefinitionUuidOrderByRequestedStartAsc(UUID classDefinitionUuid);

    List<ClassSchedulingConflict> findByClassDefinitionUuidAndIsResolvedFalseOrderByRequestedStartAsc(UUID classDefinitionUuid);
}
