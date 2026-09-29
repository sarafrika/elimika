package apps.sarafrika.elimika.classes.search;

import apps.sarafrika.elimika.classes.model.ClassDefinition;
import apps.sarafrika.elimika.classes.model.ClassSessionTemplate;
import apps.sarafrika.elimika.classes.repository.ClassDefinitionRepository;
import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.shared.search.SearchFilter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Applies a {@code classes} index filter to the database, so {@code GET /api/v1/classes?q=} filters
 * the same way whether it is served by the index or by the SQL fallback.
 * <p>
 * The filter comes from {@code SearchParamsTranslator} against {@link ClassSearchSource#DEFINITION},
 * which has already refused any attribute outside the filterable allow-list and typed every value
 * (booleans, numbers, UUIDs, and ISO dates as UTC epoch seconds). Each index attribute is mapped back
 * to what it was derived from:
 * <ul>
 *     <li>plain columns ({@code uuid}, {@code course_uuid}, ..., {@code is_active}, {@code sale_price},
 *     and the enum columns, compared by name);</li>
 *     <li>{@code created_at}: {@code created_date};</li>
 *     <li>{@code starts_at}: the earliest of the first session template's start and
 *     {@code default_start_time}, as the index computes it;</li>
 *     <li>{@code registration_closes_at}: the end (23:59:59 UTC) of {@code registration_period_end_date};</li>
 *     <li>{@code content_approved}: the linked course, else program, is approved - resolved through the
 *     course SPI once per request, like the listing's own approval check.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ClassSearchFallbackFilter {

    private static final long SECONDS_PER_DAY = 86_400L;
    private static final long LAST_SECOND_OF_DAY = SECONDS_PER_DAY - 1;

    private static final Map<String, String> COLUMNS = Map.ofEntries(
            Map.entry("uuid", "uuid"),
            Map.entry("course_uuid", "courseUuid"),
            Map.entry("program_uuid", "programUuid"),
            Map.entry("organisation_uuid", "organisationUuid"),
            Map.entry("branch_uuid", "branchUuid"),
            Map.entry("default_instructor_uuid", "defaultInstructorUuid"),
            Map.entry("category_uuid", "categoryUuid"),
            Map.entry("is_active", "isActive"),
            Map.entry("class_visibility", "classVisibility"),
            Map.entry("location_type", "locationType"),
            Map.entry("session_format", "sessionFormat"),
            Map.entry("sale_price", "salePrice"));

    private final ClassDefinitionRepository classDefinitionRepository;
    private final CourseInfoService courseInfoService;

    /** The filter as a specification, or {@code null} when there is nothing to filter. */
    public Specification<ClassDefinition> toSpecification(SearchFilter filter) {
        if (filter == null) {
            return null;
        }
        Supplier<Approval> approval = memoize(this::loadApproval);
        return (root, query, cb) -> new Translation(root, query, cb, approval).predicate(filter);
    }

    private record Approval(Set<UUID> courses, Set<UUID> programs) {
    }

    private Approval loadApproval() {
        List<UUID> courses = classDefinitionRepository.findDistinctCourseUuids();
        List<UUID> programs = classDefinitionRepository.findDistinctProgramUuids();
        return new Approval(
                courses.isEmpty() ? Set.of() : courseInfoService.findApprovedCourseUuids(courses),
                programs.isEmpty() ? Set.of() : courseInfoService.findApprovedTrainingProgramUuids(programs));
    }

    private static final class Translation {
        private final Root<ClassDefinition> root;
        private final jakarta.persistence.criteria.CriteriaQuery<?> query;
        private final CriteriaBuilder cb;
        private final Supplier<Approval> approval;

        private Translation(Root<ClassDefinition> root, jakarta.persistence.criteria.CriteriaQuery<?> query,
                            CriteriaBuilder cb, Supplier<Approval> approval) {
            this.root = root;
            this.query = query;
            this.cb = cb;
            this.approval = approval;
        }

        Predicate predicate(SearchFilter filter) {
            return switch (filter) {
                case SearchFilter.And and -> cb.and(and.filters().stream().map(this::predicate).toArray(Predicate[]::new));
                case SearchFilter.Or or -> cb.or(or.filters().stream().map(this::predicate).toArray(Predicate[]::new));
                case SearchFilter.Not not -> cb.not(predicate(not.filter()));
                case SearchFilter.IsNull isNull -> isNull(isNull.attribute());
                case SearchFilter.Eq eq -> eq(eq.attribute(), eq.value());
                case SearchFilter.In in -> cb.or(in.values().stream().map(value -> eq(in.attribute(), value))
                        .toArray(Predicate[]::new));
                case SearchFilter.Range range -> range(range);
            };
        }

        private Predicate isNull(String attribute) {
            return switch (attribute) {
                case "content_approved" -> cb.disjunction();
                case "created_at" -> root.get("createdDate").isNull();
                case "starts_at" -> startsAt().isNull();
                case "registration_closes_at" -> root.get("registrationPeriodEndDate").isNull();
                default -> column(attribute).isNull();
            };
        }

        private Predicate eq(String attribute, Object value) {
            return switch (attribute) {
                case "content_approved" -> Boolean.TRUE.equals(bool(attribute, value))
                        ? contentApproved() : cb.not(contentApproved());
                case "created_at", "starts_at", "registration_closes_at" ->
                        range(new SearchFilter.Range(attribute, value, value, null, null));
                default -> {
                    Path<Object> path = column(attribute);
                    yield cb.equal(path, convert(attribute, path.getJavaType(), value));
                }
            };
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private Predicate range(SearchFilter.Range range) {
            String attribute = range.attribute();
            if ("registration_closes_at".equals(attribute)) {
                return registrationClosesAt(range);
            }
            Expression<Comparable> expression;
            java.util.function.Function<Object, Comparable> converter;
            switch (attribute) {
                case "created_at" -> {
                    expression = (Expression) root.get("createdDate");
                    converter = value -> dateTime(attribute, value);
                }
                case "starts_at" -> {
                    expression = (Expression) startsAt();
                    converter = value -> dateTime(attribute, value);
                }
                case "sale_price" -> {
                    expression = (Expression) root.get("salePrice");
                    converter = value -> decimal(attribute, value);
                }
                default -> throw new IllegalArgumentException("Range filters are not supported on " + attribute);
            }
            List<Predicate> bounds = new java.util.ArrayList<>();
            if (range.gte() != null) {
                bounds.add(cb.greaterThanOrEqualTo(expression, converter.apply(range.gte())));
            }
            if (range.gt() != null) {
                bounds.add(cb.greaterThan(expression, converter.apply(range.gt())));
            }
            if (range.lte() != null) {
                bounds.add(cb.lessThanOrEqualTo(expression, converter.apply(range.lte())));
            }
            if (range.lt() != null) {
                bounds.add(cb.lessThan(expression, converter.apply(range.lt())));
            }
            return cb.and(bounds.toArray(Predicate[]::new));
        }

        /**
         * The index stores the last second of the end date, {@code day * 86400 + 86399}; a bound on
         * that number becomes a bound on the date column, exactly.
         */
        private Predicate registrationClosesAt(SearchFilter.Range range) {
            Path<LocalDate> date = root.get("registrationPeriodEndDate");
            List<Predicate> bounds = new java.util.ArrayList<>();
            if (range.gte() != null) {
                long x = seconds("registration_closes_at", range.gte()) - LAST_SECOND_OF_DAY;
                bounds.add(cb.greaterThanOrEqualTo(date, day(Math.ceilDiv(x, SECONDS_PER_DAY))));
            }
            if (range.gt() != null) {
                long x = seconds("registration_closes_at", range.gt()) - LAST_SECOND_OF_DAY;
                bounds.add(cb.greaterThan(date, day(Math.floorDiv(x, SECONDS_PER_DAY))));
            }
            if (range.lte() != null) {
                long x = seconds("registration_closes_at", range.lte()) - LAST_SECOND_OF_DAY;
                bounds.add(cb.lessThanOrEqualTo(date, day(Math.floorDiv(x, SECONDS_PER_DAY))));
            }
            if (range.lt() != null) {
                long x = seconds("registration_closes_at", range.lt()) - LAST_SECOND_OF_DAY;
                bounds.add(cb.lessThan(date, day(Math.ceilDiv(x, SECONDS_PER_DAY))));
            }
            return cb.and(bounds.toArray(Predicate[]::new));
        }

        /** {@code LEAST(first session template start, default_start_time)}; PostgreSQL's LEAST skips nulls. */
        private Expression<LocalDateTime> startsAt() {
            Subquery<LocalDateTime> firstSession = query.subquery(LocalDateTime.class);
            Root<ClassSessionTemplate> template = firstSession.from(ClassSessionTemplate.class);
            firstSession.select(cb.least(template.<LocalDateTime>get("startTime")))
                    .where(cb.equal(template.get("classDefinitionUuid"), root.get("uuid")));
            return cb.function("LEAST", LocalDateTime.class, firstSession, root.get("defaultStartTime"));
        }

        /** The listing's rule: the linked course, else the linked program, is approved; neither linked counts as approved. */
        private Predicate contentApproved() {
            Approval approved = approval.get();
            Path<UUID> course = root.get("courseUuid");
            Path<UUID> program = root.get("programUuid");
            Predicate courseApproved = approved.courses().isEmpty() ? cb.disjunction() : course.in(approved.courses());
            Predicate programApproved = approved.programs().isEmpty() ? cb.disjunction() : program.in(approved.programs());
            return cb.or(
                    cb.and(course.isNotNull(), courseApproved),
                    cb.and(course.isNull(), cb.or(program.isNull(), programApproved)));
        }

        private Path<Object> column(String attribute) {
            String column = COLUMNS.get(attribute);
            if (column == null) {
                throw new IllegalArgumentException("Unsupported search field: " + attribute);
            }
            return root.get(column);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convert(String attribute, Class<?> type, Object value) {
        if (type.isEnum()) {
            try {
                return Enum.valueOf((Class<? extends Enum>) type, value.toString().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid value for " + attribute);
            }
        }
        if (type == UUID.class) {
            if (value instanceof UUID uuid) {
                return uuid;
            }
            throw new IllegalArgumentException("Invalid UUID for " + attribute);
        }
        if (type == Boolean.class || type == boolean.class) {
            return bool(attribute, value);
        }
        if (type == BigDecimal.class) {
            return decimal(attribute, value);
        }
        return value;
    }

    private static Boolean bool(String attribute, Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        throw new IllegalArgumentException("Expected true or false for " + attribute);
    }

    private static BigDecimal decimal(String attribute, Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        throw new IllegalArgumentException("Expected a number for " + attribute);
    }

    private static long seconds(String attribute, Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalArgumentException("Expected an ISO date or date-time for " + attribute);
    }

    private static LocalDateTime dateTime(String attribute, Object value) {
        return LocalDateTime.ofEpochSecond(seconds(attribute, value), 0, ZoneOffset.UTC);
    }

    private static LocalDate day(long epochDay) {
        return LocalDate.ofEpochDay(epochDay);
    }

    private static <T> Supplier<T> memoize(Supplier<T> supplier) {
        Object[] value = new Object[1];
        return () -> {
            if (value[0] == null) {
                value[0] = supplier.get();
            }
            @SuppressWarnings("unchecked")
            T result = (T) value[0];
            return result;
        };
    }
}
