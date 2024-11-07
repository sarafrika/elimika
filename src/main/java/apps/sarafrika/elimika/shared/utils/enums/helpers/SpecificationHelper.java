package apps.sarafrika.elimika.shared.utils.enums.helpers;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class SpecificationHelper {

    @SafeVarargs
    public static Predicate toPredicateConditionAND(final CriteriaBuilder criteriaBuilder, final Optional<Predicate>... predicates) {
        List<Predicate> validPredicates = Arrays.stream(predicates)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        if (validPredicates.isEmpty()) {
            return criteriaBuilder.conjunction();
        }

        return criteriaBuilder.and(validPredicates.toArray(new Predicate[0]));
    }
}
