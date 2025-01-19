package apps.sarafrika.elimika.course.persistence;

import apps.sarafrika.elimika.course.dto.request.CourseRequestDTO;
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
public class CourseSpecification implements Specification<Course> {

    private final CourseRequestDTO courseRequestDTO;

    @Override
    public Predicate toPredicate(Root<Course> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        return SpecificationHelper.toPredicateConditionAND(
                criteriaBuilder,
                searchByName(root, criteriaBuilder)
        );
    }

    private Optional<Predicate> searchByName(Root<Course> root, CriteriaBuilder criteriaBuilder) {

        return Optional.ofNullable(courseRequestDTO.name())
                .map(name -> criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
    }
}
