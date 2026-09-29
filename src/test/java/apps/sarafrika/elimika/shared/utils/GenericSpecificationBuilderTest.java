package apps.sarafrika.elimika.shared.utils;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenericSpecificationBuilderTest {

    private GenericSpecificationBuilder<TestEntity> builder;

    @Mock
    private Root<TestEntity> root;

    @Mock
    private Path<Object> path;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private CriteriaQuery<Object> criteriaQuery;

    @Mock
    private Predicate predicate;

    @BeforeEach
    void setUp() {
        builder = new GenericSpecificationBuilder<>();
    }

    @Test
    void buildSpecificationHandlesFieldsWithUnderscores() {
        Map<String, String> searchParams = Map.of("admin_verified", "true");

        Specification<TestEntity> specification = builder.buildSpecification(TestEntity.class, searchParams);
        assertThat(specification).isNotNull();

        when(root.get("adminVerified")).thenReturn(path);
        when(path.getJavaType()).thenAnswer(invocation -> Boolean.class);
        when(criteriaBuilder.equal(path, true)).thenReturn(predicate);

        specification.toPredicate(root, criteriaQuery, criteriaBuilder);

        verify(root).get("adminVerified");
        verify(criteriaBuilder).equal(path, true);
    }

    @Test
    void buildSpecificationRejectsFieldThatIsNotExposed() {
        Map<String, String> searchParams = Map.of("price_gte", "100");

        assertThatThrownBy(() -> builder.buildSpecification(TestEntity.class, searchParams))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("price");
    }

    @Test
    void buildSpecificationRejectsSortOnFieldThatIsNotExposed() {
        Map<String, String> searchParams = Map.of("admin_verified", "true", "sort", "price,desc");

        assertThatThrownBy(() -> builder.buildSpecification(TestEntity.class, searchParams))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported sort property");
    }

    @Test
    void validateSortPropertiesRejectsEverySortOrderNotOnlyTheFirst() {
        PageRequest pageable = PageRequest.of(0, 20,
                Sort.by(Sort.Order.asc("adminVerified"), Sort.Order.desc("price")));

        assertThatThrownBy(() -> builder.validateSortProperties(TestEntity.class, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("price");
    }

    @Test
    void validateSortPropertiesAcceptsExposedFieldInEitherSpelling() {
        builder.validateSortProperties(TestEntity.class,
                PageRequest.of(0, 20, Sort.by("adminVerified")));
        builder.validateSortProperties(TestEntity.class,
                PageRequest.of(0, 20, Sort.by("admin_verified")));
    }

    @Test
    void validateSortPropertiesRejectsColumnAliasThatIsNotAJpaProperty() {
        PageRequest pageable = PageRequest.of(0, 20, Sort.by("owner_id"));

        assertThatThrownBy(() -> builder.validateSortProperties(TestEntity.class, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported sort property");
    }

    @Test
    @SuppressWarnings("unchecked")
    void likeOperationsEscapeWildcardsInUserInput() {
        Expression<String> stringExpression = mock(Expression.class);
        Expression<String> lowerExpression = mock(Expression.class);
        when(root.get("owner")).thenReturn(path);
        when(path.getJavaType()).thenAnswer(invocation -> String.class);
        when(path.as(String.class)).thenReturn(stringExpression);
        when(criteriaBuilder.lower(stringExpression)).thenReturn(lowerExpression);

        builder.buildSpecification(TestEntity.class, Map.of("owner_like", "50%_Off\\"))
                .toPredicate(root, criteriaQuery, criteriaBuilder);
        builder.buildSpecification(TestEntity.class, Map.of("owner_startswith", "a_"))
                .toPredicate(root, criteriaQuery, criteriaBuilder);
        builder.buildSpecification(TestEntity.class, Map.of("owner_endswith", "%z"))
                .toPredicate(root, criteriaQuery, criteriaBuilder);

        verify(criteriaBuilder).like(lowerExpression, "%50\\%\\_off\\\\%", '\\');
        verify(criteriaBuilder).like(lowerExpression, "a\\_%", '\\');
        verify(criteriaBuilder).like(lowerExpression, "%\\%z", '\\');
    }

    @Test
    void buildSpecificationReturnsNullWithoutCriteria() {
        assertThat(builder.buildSpecification(TestEntity.class, Map.of())).isNull();
    }

    @Entity
    private static class TestEntity extends BaseEntity {
        @Column(name = "admin_verified")
        @Filterable
        private Boolean adminVerified;

        @Column(name = "owner_id")
        @Filterable
        private String owner;

        @Column(name = "price")
        private BigDecimal price;
    }
}
