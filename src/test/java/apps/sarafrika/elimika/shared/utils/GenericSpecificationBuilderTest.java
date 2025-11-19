package apps.sarafrika.elimika.shared.utils;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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
        when(path.getJavaType()).thenReturn(Boolean.class);
        when(criteriaBuilder.equal(path, true)).thenReturn(predicate);

        specification.toPredicate(root, criteriaQuery, criteriaBuilder);

        verify(root).get("adminVerified");
        verify(criteriaBuilder).equal(path, true);
    }

    @Entity
    private static class TestEntity extends BaseEntity {
        @Column(name = "admin_verified")
        private Boolean adminVerified;
    }
}

