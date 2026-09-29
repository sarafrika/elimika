package apps.sarafrika.elimika.shared.utils;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SortAllowListTest {

    private static final Set<String> ALLOWED = Set.of("createdDate", "title");

    @Test
    void acceptsAllowedPropertiesInEitherSpelling() {
        assertThatCode(() -> SortAllowList.validate(
                PageRequest.of(0, 20, Sort.by("created_date").descending().and(Sort.by("TITLE"))), ALLOWED))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsUnsortedAndUnpagedRequests() {
        assertThatCode(() -> SortAllowList.validate(PageRequest.of(0, 20), ALLOWED)).doesNotThrowAnyException();
        assertThatCode(() -> SortAllowList.validate(Pageable.unpaged(), ALLOWED)).doesNotThrowAnyException();
    }

    @Test
    void rejectsPropertiesOutsideTheAllowList() {
        assertThatThrownBy(() -> SortAllowList.validate(PageRequest.of(0, 20, Sort.by("sale_price")), ALLOWED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported sort property: sale_price");
    }
}
