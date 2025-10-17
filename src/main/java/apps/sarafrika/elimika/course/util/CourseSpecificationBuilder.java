package apps.sarafrika.elimika.course.util;

import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseCategoryMapping;
import apps.sarafrika.elimika.course.model.Category;
import apps.sarafrika.elimika.course.model.DifficultyLevel;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Custom specification builder for Course entity searches.
 * Handles complex queries including computed fields, relationships,
 * and domain-specific search criteria.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-16
 */
@Component
@Slf4j
public class CourseSpecificationBuilder {

    private final GenericSpecificationBuilder<Course> genericSpecificationBuilder;

    public CourseSpecificationBuilder(GenericSpecificationBuilder<Course> genericSpecificationBuilder) {
        this.genericSpecificationBuilder = genericSpecificationBuilder;
    }

    /**
     * Builds a comprehensive specification from search parameters.
     * Supports both generic field searches and custom course-specific searches.
     */
    public Specification<Course> buildCourseSpecification(Map<String, String> searchParams) {
        if (searchParams == null || searchParams.isEmpty()) {
            return null;
        }

        List<Specification<Course>> specifications = new ArrayList<>();

        // Extract custom parameters that need special handling
        String categoryName = searchParams.get("category_name");
        String difficultyName = searchParams.get("difficulty_name");
        String lifecycleStage = searchParams.get("lifecycle_stage");
        String isFree = searchParams.get("is_free");
        String isPublished = searchParams.get("is_published");
        String isDraft = searchParams.get("is_draft");
        String isArchived = searchParams.get("is_archived");
        String isInReview = searchParams.get("is_in_review");
        String minPrice = searchParams.get("min_price");
        String maxPrice = searchParams.get("max_price");
        String minTrainingFee = searchParams.get("min_training_fee");
        String maxTrainingFee = searchParams.get("max_training_fee");
        String hasEnrollments = searchParams.get("has_enrollments");
        String acceptsNewEnrollments = searchParams.get("accepts_new_enrollments");
        String courseCreatorUuid = searchParams.get("course_creator_uuid");
        String active = searchParams.get("active");

        // Handle category name search
        if (categoryName != null && !categoryName.trim().isEmpty()) {
            specifications.add(hasCategoryName(categoryName.trim()));
        }

        // Handle difficulty name search
        if (difficultyName != null && !difficultyName.trim().isEmpty()) {
            specifications.add(hasDifficultyName(difficultyName.trim()));
        }

        // Handle lifecycle stage (computed field)
        if (lifecycleStage != null && !lifecycleStage.trim().isEmpty()) {
            specifications.add(hasLifecycleStage(lifecycleStage.trim()));
        }

        // Handle is_free (computed field)
        if (isFree != null) {
            boolean free = Boolean.parseBoolean(isFree);
            specifications.add(isFree(free));
        }

        // Handle status-based searches
        if (isPublished != null && Boolean.parseBoolean(isPublished)) {
            specifications.add(hasStatus(ContentStatus.PUBLISHED));
        }
        if (isDraft != null && Boolean.parseBoolean(isDraft)) {
            specifications.add(hasStatus(ContentStatus.DRAFT));
        }
        if (isArchived != null && Boolean.parseBoolean(isArchived)) {
            specifications.add(hasStatus(ContentStatus.ARCHIVED));
        }
        if (isInReview != null && Boolean.parseBoolean(isInReview)) {
            specifications.add(hasStatus(ContentStatus.IN_REVIEW));
        }

        // Handle price range
        if (minPrice != null) {
            try {
                BigDecimal min = new BigDecimal(minPrice);
                specifications.add(priceGreaterThanOrEqual(min));
            } catch (NumberFormatException e) {
                log.warn("Invalid min_price value: {}", minPrice);
            }
        }
        if (maxPrice != null) {
            try {
                BigDecimal max = new BigDecimal(maxPrice);
                specifications.add(priceLessThanOrEqual(max));
            } catch (NumberFormatException e) {
                log.warn("Invalid max_price value: {}", maxPrice);
            }
        }

        if (minTrainingFee != null) {
            try {
                BigDecimal minFee = new BigDecimal(minTrainingFee);
                specifications.add(minimumTrainingFeeGreaterThanOrEqual(minFee));
            } catch (NumberFormatException e) {
                log.warn("Invalid min_training_fee value: {}", minTrainingFee);
            }
        }

        if (maxTrainingFee != null) {
            try {
                BigDecimal maxFee = new BigDecimal(maxTrainingFee);
                specifications.add(minimumTrainingFeeLessThanOrEqual(maxFee));
            } catch (NumberFormatException e) {
                log.warn("Invalid max_training_fee value: {}", maxTrainingFee);
            }
        }

        // Handle enrollment-based searches
        if (hasEnrollments != null) {
            boolean withEnrollments = Boolean.parseBoolean(hasEnrollments);
            specifications.add(hasEnrollments(withEnrollments));
        }

        // Handle accepts_new_enrollments (computed field based on class_limit)
        if (acceptsNewEnrollments != null) {
            boolean accepts = Boolean.parseBoolean(acceptsNewEnrollments);
            specifications.add(acceptsNewEnrollments(accepts));
        }

        // Handle course creator UUID
        if (courseCreatorUuid != null && !courseCreatorUuid.trim().isEmpty()) {
            try {
                UUID creatorUuid = UUID.fromString(courseCreatorUuid.trim());
                specifications.add(hasCourseCreator(creatorUuid));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid course_creator_uuid value: {}", courseCreatorUuid);
            }
        }

        // Handle active status
        if (active != null) {
            boolean isActive = Boolean.parseBoolean(active);
            specifications.add(isActive(isActive));
        }

        // Use generic specification helper for remaining standard fields
        Map<String, String> remainingParams = new java.util.HashMap<>(searchParams);
        remainingParams.remove("category_name");
        remainingParams.remove("difficulty_name");
        remainingParams.remove("lifecycle_stage");
        remainingParams.remove("is_free");
        remainingParams.remove("is_published");
        remainingParams.remove("is_draft");
        remainingParams.remove("is_archived");
        remainingParams.remove("is_in_review");
        remainingParams.remove("min_price");
        remainingParams.remove("max_price");
        remainingParams.remove("min_training_fee");
        remainingParams.remove("max_training_fee");
        remainingParams.remove("has_enrollments");
        remainingParams.remove("accepts_new_enrollments");
        remainingParams.remove("course_creator_uuid");
        remainingParams.remove("active");

        if (!remainingParams.isEmpty()) {
            Specification<Course> genericSpec = genericSpecificationBuilder.buildSpecification(Course.class, remainingParams);
            if (genericSpec != null) {
                specifications.add(genericSpec);
            }
        }

        // Combine all specifications with AND logic
        if (specifications.isEmpty()) {
            return null;
        }

        return specifications.stream()
                .reduce((spec1, spec2) -> spec1 == null ? spec2 : spec1.and(spec2))
                .orElse(null);
    }

    /**
     * Search courses by category name.
     * Uses subquery to check the course_category_mappings junction table.
     */
    public Specification<Course> hasCategoryName(String categoryName) {
        return (root, query, criteriaBuilder) -> {
            Subquery<UUID> subquery = query.subquery(UUID.class);
            Root<CourseCategoryMapping> mappingRoot = subquery.from(CourseCategoryMapping.class);
            Join<CourseCategoryMapping, Category> categoryJoin = mappingRoot.join("category");

            subquery.select(mappingRoot.get("courseUuid"))
                    .where(criteriaBuilder.like(
                            criteriaBuilder.lower(categoryJoin.get("name")),
                            "%" + categoryName.toLowerCase() + "%"
                    ));

            return criteriaBuilder.in(root.get("uuid")).value(subquery);
        };
    }

    /**
     * Search courses by difficulty level name.
     */
    public Specification<Course> hasDifficultyName(String difficultyName) {
        return (root, query, criteriaBuilder) -> {
            Join<Course, DifficultyLevel> difficultyJoin = root.join("difficulty", JoinType.LEFT);
            return criteriaBuilder.like(
                    criteriaBuilder.lower(difficultyJoin.get("name")),
                    "%" + difficultyName.toLowerCase() + "%"
            );
        };
    }

    /**
     * Search courses by lifecycle stage (computed field based on status).
     * Maps lifecycle stages to ContentStatus values.
     */
    public Specification<Course> hasLifecycleStage(String lifecycleStage) {
        return (root, query, criteriaBuilder) -> {
            ContentStatus status = switch (lifecycleStage.toLowerCase()) {
                case "draft" -> ContentStatus.DRAFT;
                case "in_review" -> ContentStatus.IN_REVIEW;
                case "published" -> ContentStatus.PUBLISHED;
                case "archived" -> ContentStatus.ARCHIVED;
                default -> null;
            };

            if (status == null) {
                log.warn("Unknown lifecycle stage: {}", lifecycleStage);
                return criteriaBuilder.disjunction(); // Returns no results
            }

            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    /**
     * Filter courses by free status (price is null or 0).
     */
    public Specification<Course> isFree(boolean free) {
        return (root, query, criteriaBuilder) -> {
            if (free) {
                return criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("price")),
                        criteriaBuilder.equal(root.get("price"), BigDecimal.ZERO)
                );
            } else {
                return criteriaBuilder.and(
                        criteriaBuilder.isNotNull(root.get("price")),
                        criteriaBuilder.greaterThan(root.get("price"), BigDecimal.ZERO)
                );
            }
        };
    }

    /**
     * Filter courses by content status.
     */
    public Specification<Course> hasStatus(ContentStatus status) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status);
    }

    /**
     * Filter courses with price greater than or equal to minimum.
     */
    public Specification<Course> priceGreaterThanOrEqual(BigDecimal minPrice) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    /**
     * Filter courses with price less than or equal to maximum.
     */
    public Specification<Course> priceLessThanOrEqual(BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    public Specification<Course> minimumTrainingFeeGreaterThanOrEqual(BigDecimal minFee) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("minimumTrainingFee"), minFee);
    }

    public Specification<Course> minimumTrainingFeeLessThanOrEqual(BigDecimal maxFee) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("minimumTrainingFee"), maxFee);
    }

    /**
     * Filter courses based on whether they have enrollments.
     */
    public Specification<Course> hasEnrollments(boolean withEnrollments) {
        return (root, query, criteriaBuilder) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<CourseEnrollment> enrollmentRoot = subquery.from(CourseEnrollment.class);

            subquery.select(criteriaBuilder.count(enrollmentRoot.get("uuid")))
                    .where(criteriaBuilder.equal(
                            enrollmentRoot.get("courseUuid"),
                            root.get("uuid")
                    ));

            if (withEnrollments) {
                return criteriaBuilder.greaterThan(subquery, 0L);
            } else {
                return criteriaBuilder.or(
                        criteriaBuilder.equal(subquery, 0L),
                        criteriaBuilder.isNull(subquery)
                );
            }
        };
    }

    /**
     * Filter courses that accept new enrollments.
     * A course accepts new enrollments if:
     * - It's published
     * - It's active
     * - Either class_limit is null OR current enrollments < class_limit
     */
    public Specification<Course> acceptsNewEnrollments(boolean accepts) {
        return (root, query, criteriaBuilder) -> {
            if (!accepts) {
                return criteriaBuilder.conjunction(); // No filter if false
            }

            // Count current enrollments
            Subquery<Long> enrollmentCountSubquery = query.subquery(Long.class);
            Root<CourseEnrollment> enrollmentRoot = enrollmentCountSubquery.from(CourseEnrollment.class);
            enrollmentCountSubquery.select(criteriaBuilder.count(enrollmentRoot.get("uuid")))
                    .where(criteriaBuilder.equal(
                            enrollmentRoot.get("courseUuid"),
                            root.get("uuid")
                    ));

            return criteriaBuilder.and(
                    criteriaBuilder.equal(root.get("status"), ContentStatus.PUBLISHED),
                    criteriaBuilder.equal(root.get("active"), true),
                    criteriaBuilder.or(
                            criteriaBuilder.isNull(root.get("classLimit")),
                            criteriaBuilder.lessThan(enrollmentCountSubquery, root.get("classLimit"))
                    )
            );
        };
    }

    /**
     * Filter courses by course creator UUID.
     */
    public Specification<Course> hasCourseCreator(UUID courseCreatorUuid) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("courseCreatorUuid"), courseCreatorUuid);
    }

    /**
     * Filter courses by active status.
     */
    public Specification<Course> isActive(boolean active) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("active"), active);
    }

    /**
     * Filter courses by multiple category UUIDs (OR logic).
     */
    public Specification<Course> hasCategories(List<UUID> categoryUuids) {
        return (root, query, criteriaBuilder) -> {
            Subquery<UUID> subquery = query.subquery(UUID.class);
            Root<CourseCategoryMapping> mappingRoot = subquery.from(CourseCategoryMapping.class);

            subquery.select(mappingRoot.get("courseUuid"))
                    .where(mappingRoot.get("categoryUuid").in(categoryUuids));

            return criteriaBuilder.in(root.get("uuid")).value(subquery);
        };
    }

    /**
     * Filter courses by duration range (total minutes).
     */
    public Specification<Course> hasDurationInRange(Integer minMinutes, Integer maxMinutes) {
        return (root, query, criteriaBuilder) -> {
            // Calculate total minutes as (durationHours * 60 + durationMinutes)
            Expression<Integer> totalMinutes = criteriaBuilder.sum(
                    criteriaBuilder.prod(
                            criteriaBuilder.coalesce(root.get("durationHours"), 0),
                            60
                    ),
                    criteriaBuilder.coalesce(root.get("durationMinutes"), 0)
            );

            List<Predicate> predicates = new ArrayList<>();
            if (minMinutes != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(totalMinutes, minMinutes));
            }
            if (maxMinutes != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(totalMinutes, maxMinutes));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Filter courses by age range compatibility.
     */
    public Specification<Course> isCompatibleWithAge(Integer age) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("ageLowerLimit")),
                        criteriaBuilder.lessThanOrEqualTo(root.get("ageLowerLimit"), age)
                ),
                criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("ageUpperLimit")),
                        criteriaBuilder.greaterThanOrEqualTo(root.get("ageUpperLimit"), age)
                )
        );
    }
}
