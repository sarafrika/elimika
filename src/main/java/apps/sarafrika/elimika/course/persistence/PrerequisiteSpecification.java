package apps.sarafrika.elimika.course.persistence;

import apps.sarafrika.elimika.course.dto.request.PrerequisiteRequestDTO;
import apps.sarafrika.elimika.shared.utils.enums.helpers.SpecificationHelper;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

@Builder
@AllArgsConstructor
public class PrerequisiteSpecification implements Specification<Prerequisite> {

    private final PrerequisiteRequestDTO prerequisiteRequestDTO;

    @Override
    public Predicate toPredicate(Root<Prerequisite> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {

        return SpecificationHelper.toPredicateConditionAND(
                criteriaBuilder,
                searchByCourseId(root, criteriaBuilder),
                searchByLessonId(root, criteriaBuilder)
        );
    }

    private Optional<Predicate> searchByCourseId(Root<Prerequisite> root, CriteriaBuilder criteriaBuilder) {

        return Optional.of(criteriaBuilder.equal(root.get("courseId"), prerequisiteRequestDTO.courseId()));
    }

    private Optional<Predicate> searchByLessonId(Root<Prerequisite> root, CriteriaBuilder criteriaBuilder) {

        return Optional.of(criteriaBuilder.equal(root.get("lessonId"), prerequisiteRequestDTO.lessonId()));
    }
}
