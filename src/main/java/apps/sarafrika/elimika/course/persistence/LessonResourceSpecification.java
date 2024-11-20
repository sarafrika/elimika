package apps.sarafrika.elimika.course.persistence;

import apps.sarafrika.elimika.course.dto.request.LessonResouceRequestDTO;
import apps.sarafrika.elimika.shared.utils.helpers.SpecificationHelper;
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
public class LessonResourceSpecification implements Specification<LessonResource> {

    private final LessonResouceRequestDTO lessonResouceRequestDTO;

    @Override
    public Predicate toPredicate(Root<LessonResource> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {

        return SpecificationHelper.toPredicateConditionAND(
                criteriaBuilder,
                searchByLessonId(root, criteriaBuilder)
        );
    }

    private Optional<Predicate> searchByLessonId(Root<LessonResource> root, CriteriaBuilder criteriaBuilder) {

        return Optional.ofNullable(lessonResouceRequestDTO.lessonId())
                .map(lessonId -> criteriaBuilder.equal(root.get("lessonId"), lessonId));
    }
}
