package apps.sarafrika.elimika.course.repository;

import apps.sarafrika.elimika.course.model.LearnerSkillEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LearnerSkillEvidenceRepository extends JpaRepository<LearnerSkillEvidence, Long> {

    Optional<LearnerSkillEvidence> findFirstByStudentUuidAndProgramUuidAndSkillUuidAndLineItemUuidAndSupersededFalse(
            UUID studentUuid, UUID programUuid, UUID skillUuid, UUID lineItemUuid);

    Optional<LearnerSkillEvidence> findFirstByStudentUuidAndProgramUuidAndSkillUuidAndSupersededFalseOrderByGradedAtDescIdDesc(
            UUID studentUuid, UUID programUuid, UUID skillUuid);

    List<LearnerSkillEvidence> findByStudentUuidAndProgramUuidOrderByGradedAtDescIdDesc(UUID studentUuid, UUID programUuid);
}
