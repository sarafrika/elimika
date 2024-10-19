package apps.sarafrika.elimika.shared.utils.enums.helpers;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;

import java.util.Arrays;
import java.util.Optional;

public class SpecificationHelper {

    @SafeVarargs
    public static Predicate toPredicateConditionAND(final CriteriaBuilder criteriaBuilder, final Optional<Predicate>... predicates) {

        return Arrays.stream(predicates)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .reduce(criteriaBuilder::and)
                .orElse(null);
    }
}
