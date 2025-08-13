package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.RubricCriteriaDTO;
import apps.sarafrika.elimika.course.model.RubricCriteria;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RubricCriteriaFactory {

    // Convert RubricCriteria entity to RubricCriteriaDTO
    public static RubricCriteriaDTO toDTO(RubricCriteria rubricCriteria) {
        if (rubricCriteria == null) {
            return null;
        }
        return new RubricCriteriaDTO(
                rubricCriteria.getUuid(),
                rubricCriteria.getRubricUuid(),
                rubricCriteria.getComponentName(),
                rubricCriteria.getDescription(),
                rubricCriteria.getDisplayOrder(),
                rubricCriteria.getWeight(),
                rubricCriteria.getCreatedDate(),
                rubricCriteria.getCreatedBy(),
                rubricCriteria.getLastModifiedDate(),
                rubricCriteria.getLastModifiedBy()
        );
    }

    // Convert RubricCriteriaDTO to RubricCriteria entity
    public static RubricCriteria toEntity(RubricCriteriaDTO dto) {
        if (dto == null) {
            return null;
        }
        RubricCriteria rubricCriteria = new RubricCriteria();
        rubricCriteria.setUuid(dto.uuid());
        rubricCriteria.setRubricUuid(dto.rubricUuid());
        rubricCriteria.setComponentName(dto.componentName());
        rubricCriteria.setDescription(dto.description());
        rubricCriteria.setDisplayOrder(dto.displayOrder());
        rubricCriteria.setWeight(dto.weight());
        rubricCriteria.setCreatedDate(dto.createdDate());
        rubricCriteria.setCreatedBy(dto.createdBy());
        rubricCriteria.setLastModifiedDate(dto.updatedDate());
        rubricCriteria.setLastModifiedBy(dto.updatedBy());
        return rubricCriteria;
    }
}