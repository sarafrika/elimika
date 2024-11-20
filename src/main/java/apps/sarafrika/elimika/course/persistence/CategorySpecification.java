package apps.sarafrika.elimika.course.persistence;

import apps.sarafrika.elimika.course.dto.request.CategoryRequestDTO;
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
public class CategorySpecification implements Specification<Category> {

    private final CategoryRequestDTO categoryRequestDTO;

    @Override
    public Predicate toPredicate(Root<Category> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {

        return SpecificationHelper.toPredicateConditionAND(
                criteriaBuilder,
                searchByName(root, criteriaBuilder)
        );
    }

    private Optional<Predicate> searchByName(Root<Category> root, CriteriaBuilder criteriaBuilder) {

        return Optional.ofNullable(categoryRequestDTO.name())
                .map(name -> criteriaBuilder.like(root.get("name"), "%" + name + "%"));
    }
}
