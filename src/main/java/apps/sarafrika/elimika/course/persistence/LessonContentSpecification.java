package apps.sarafrika.elimika.course.persistence;

import apps.sarafrika.elimika.course.dto.request.LessonContentRequestDTO;
import apps.sarafrika.elimika.shared.utils.helpers.SpecificationHelper;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

@Builder
@RequiredArgsConstructor
public class LessonContentSpecification implements Specification<LessonContent> {

    private final LessonContentRequestDTO lessonContentRequestDTO;

    @Override
    public Predicate toPredicate(Root<LessonContent> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {

        return SpecificationHelper.toPredicateConditionAND(
                criteriaBuilder,
                searchByLessonId(root, criteriaBuilder)
        );
    }

    private Optional<Predicate> searchByLessonId(Root<LessonContent> root, CriteriaBuilder criteriaBuilder) {

        return Optional.ofNullable(lessonContentRequestDTO.lessonId())
                .map(lessonId -> criteriaBuilder.equal(root.get("lessonId"), lessonId));
    }
}
