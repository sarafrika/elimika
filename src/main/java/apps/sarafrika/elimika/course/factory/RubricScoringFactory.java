package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.RubricScoringDTO;
import apps.sarafrika.elimika.course.model.RubricScoring;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RubricScoringFactory {

    // Convert RubricScoring entity to RubricScoringDTO
    public static RubricScoringDTO toDTO(RubricScoring rubricScoring) {
        if (rubricScoring == null) {
            return null;
        }
        return new RubricScoringDTO(
                rubricScoring.getUuid(),
                rubricScoring.getCriteriaUuid(),
                rubricScoring.getDescription(),
                rubricScoring.getCreatedDate(),
                rubricScoring.getCreatedBy(),
                rubricScoring.getLastModifiedDate(),
                rubricScoring.getLastModifiedBy()
        );
    }

    // Convert RubricScoringDTO to RubricScoring entity
    public static RubricScoring toEntity(RubricScoringDTO dto) {
        if (dto == null) {
            return null;
        }
        RubricScoring rubricScoring = new RubricScoring();
        rubricScoring.setUuid(dto.uuid());
        rubricScoring.setCriteriaUuid(dto.criteriaUuid());
        rubricScoring.setDescription(dto.description());
        rubricScoring.setCreatedDate(dto.createdDate());
        rubricScoring.setCreatedBy(dto.createdBy());
        rubricScoring.setLastModifiedDate(dto.updatedDate());
        rubricScoring.setLastModifiedBy(dto.updatedBy());
        return rubricScoring;
    }
}