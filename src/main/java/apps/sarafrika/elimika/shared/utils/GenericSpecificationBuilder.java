package apps.sarafrika.elimika.shared.utils;

import jakarta.persistence.Column;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds JPA {@link Specification}s from a flat {@code searchParams} request map.
 * <p>
 * Filtering and sorting are default-deny: a request key is only honoured when it resolves to an
 * entity field annotated with {@link Filterable}. Anything else - an unknown column, a typo, or a
 * column that exists but is deliberately not exposed - raises {@link IllegalArgumentException},
 * which the global handler turns into a 400. Silently dropping such keys would leak the shape of
 * the table through the resulting row counts.
 */
@Component
@Slf4j
public class GenericSpecificationBuilder<T> {
    private static final String SORT_PARAM = "sort";
    private static final List<String> EXCLUDED_PARAMS = List.of("page", "size", SORT_PARAM);
    private static final Set<String> SORT_DIRECTION_TOKENS = Set.of("asc", "desc", "ignorecase");
    private static final int MAX_ECHOED_KEY_LENGTH = 64;
    private static final Set<String> SUPPORTED_OPERATIONS = Set.of(
            "eq",
            "gt",
            "lt",
            "gte",
            "lte",
            "like",
            "startswith",
            "endswith",
            "in",
            "notin",
            "noteq",
            "between",
            "notingroup"
    );
    private final Map<Class<?>, Map<String, String>> fieldColumnCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, Set<String>> sortablePropertyCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, Map<String, Class<?>>> relationshipCache = new ConcurrentHashMap<>();
    private final Map<RelationshipKey, String> inverseRelationshipCache = new ConcurrentHashMap<>();

    private record RelationshipKey(Class<?> entityClass, Class<?> targetClass) {
    }

    public Specification<T> buildSpecification(Class<T> entityClass, Map<String, String> searchParams) {
        Map<String, String> fieldColumnMap = getFieldColumnMap(entityClass);
        if (searchParams != null) {
            validateSortExpression(entityClass, searchParams.get(SORT_PARAM));
        }
        List<SearchCriteria> criteriaList = buildSearchCriteria(searchParams, fieldColumnMap);
        return criteriaList.isEmpty() ? null : createSpecification(criteriaList);
    }

    /**
     * Validates every ordering carried by a bound {@link Pageable} against the allow-list.
     * <p>
     * Sorting is as good an oracle as filtering: ordering a page by an unexposed column ranks the
     * whole table by that column. The bound {@code Pageable} is the only place where <em>all</em>
     * orderings are visible - a repeated {@code ?sort=} parameter collapses to its first value in
     * the {@code searchParams} map while Spring still applies every one of them - so services must
     * validate here, including on endpoints that build their own {@code searchParams} map.
     *
     * @throws IllegalArgumentException when a sort property is not annotated {@link Filterable}
     */
    public void validateSortProperties(Class<?> entityClass, Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return;
        }
        validateSortProperties(entityClass, pageable.getSort());
    }

    private void validateSortProperties(Class<?> entityClass, Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return;
        }
        for (Sort.Order order : sort) {
            validateSortProperty(entityClass, order.getProperty());
        }
    }

    /**
     * Validates the raw {@code sort} request value that rides in the {@code searchParams} map, for
     * endpoints whose caller passes the sort as a query parameter rather than a bound {@link Pageable}.
     */
    private void validateSortExpression(Class<?> entityClass, String sortExpression) {
        if (sortExpression == null || sortExpression.isBlank()) {
            return;
        }
        for (String token : sortExpression.split(",")) {
            String property = token.trim();
            if (property.isEmpty() || SORT_DIRECTION_TOKENS.contains(property.toLowerCase(Locale.ROOT))) {
                continue;
            }
            validateSortProperty(entityClass, property);
        }
    }

    /**
     * A sort property is checked against JPA property names rather than the filter alias map: the
     * alias map also holds column names, and a column name whose entity field is spelled differently
     * ({@code organisation_id} for the field {@code organisation}) would pass the check and then blow
     * up inside Spring Data as a 500 instead of being rejected as a 400.
     */
    private void validateSortProperty(Class<?> entityClass, String property) {
        if (!getSortableProperties(entityClass).contains(normaliseSortProperty(property))) {
            throw new IllegalArgumentException("Unsupported sort property: " + sanitiseForMessage(property));
        }
    }

    /**
     * Spring Data resolves {@code created_date} to the field {@code createdDate}, so underscores are
     * insignificant when matching a requested sort property to a field name.
     */
    private String normaliseSortProperty(String property) {
        return property.replace("_", "").toLowerCase(Locale.ROOT);
    }

    private Set<String> getSortableProperties(Class<?> entityClass) {
        return sortablePropertyCache.computeIfAbsent(entityClass, this::buildSortableProperties);
    }

    private Set<String> buildSortableProperties(Class<?> entityClass) {
        Set<String> properties = new HashSet<>();
        Class<?> currentClass = entityClass;
        while (currentClass != null) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Filterable.class)) {
                    properties.add(normaliseSortProperty(field.getName()));
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        return Collections.unmodifiableSet(properties);
    }

    /**
     * Echoes a rejected request key back to the caller without letting arbitrary payload through:
     * anything outside an identifier is dropped and the result is capped.
     */
    private String sanitiseForMessage(String key) {
        String cleaned = key.replaceAll("[^A-Za-z0-9_.]", "");
        return cleaned.length() > MAX_ECHOED_KEY_LENGTH ? cleaned.substring(0, MAX_ECHOED_KEY_LENGTH) : cleaned;
    }

    private Map<String, String> getFieldColumnMap(Class<?> entityClass) {
        return fieldColumnCache.computeIfAbsent(entityClass, this::buildFieldColumnMap);
    }

    private List<SearchCriteria> buildSearchCriteria(Map<String, String> searchParams, Map<String, String> fieldColumnMap) {
        List<SearchCriteria> criteriaList = new ArrayList<>();
        searchParams.forEach((key, value) -> {
            if (isValidSearchParam(key, value)) {
                addSearchCriteria(criteriaList, key, value, fieldColumnMap);
            }
        });
        return criteriaList;
    }

    private boolean isValidSearchParam(String key, String value) {
        return value != null && !value.isEmpty() && !EXCLUDED_PARAMS.contains(key.toLowerCase());
    }

    private void addSearchCriteria(List<SearchCriteria> criteriaList, String key, String value, Map<String, String> fieldColumnMap) {
        SearchCriteriaInfo criteriaInfo = parseSearchKey(key);
        String resolvedField = fieldColumnMap.get(criteriaInfo.fieldName().toLowerCase());
        if (resolvedField == null) {
            throw new IllegalArgumentException("Unsupported search field: " + sanitiseForMessage(criteriaInfo.fieldName()));
        }

        criteriaList.add(new SearchCriteria(
                resolvedField,
                criteriaInfo.operation(),
                value
        ));
    }

    private record SearchCriteriaInfo(String fieldName, String operation) {
    }

    private SearchCriteriaInfo parseSearchKey(String key) {
        int lastUnderscoreIndex = key.lastIndexOf("_");
        if (lastUnderscoreIndex != -1 && lastUnderscoreIndex < key.length() - 1) {
            String potentialOperation = key.substring(lastUnderscoreIndex + 1).toLowerCase(Locale.ROOT);
            if (SUPPORTED_OPERATIONS.contains(potentialOperation)) {
                return new SearchCriteriaInfo(
                        key.substring(0, lastUnderscoreIndex),
                        potentialOperation
                );
            }
        }
        return new SearchCriteriaInfo(key, "eq");
    }

    private Map<String, String> buildFieldColumnMap(Class<?> entityClass) {
        Map<String, String> fieldColumnMap = new HashMap<>();
        processClassHierarchy(entityClass, fieldColumnMap);
        return fieldColumnMap;
    }

    private void processClassHierarchy(Class<?> entityClass, Map<String, String> fieldColumnMap) {
        Class<?> currentClass = entityClass;
        while (currentClass != null) {
            processFields(currentClass.getDeclaredFields(), fieldColumnMap);
            currentClass = currentClass.getSuperclass();
        }
    }

    private void processFields(Field[] fields, Map<String, String> fieldColumnMap) {
        for (Field field : fields) {
            processField(field, fieldColumnMap);
        }
    }

    private void processField(Field field, Map<String, String> fieldColumnMap) {
        if (!field.isAnnotationPresent(Filterable.class)) {
            return;
        }

        String fieldName = field.getName();
        fieldColumnMap.put(fieldName.toLowerCase(), fieldName);

        processColumnAnnotation(field, fieldColumnMap);
        processJoinColumn(field, fieldColumnMap);
        processRelationshipAnnotations(field, fieldColumnMap);
    }

    private void processColumnAnnotation(Field field, Map<String, String> fieldColumnMap) {
        String columnName = getColumnAnnotationName(field);
        if (columnName != null) {
            fieldColumnMap.put(columnName.toLowerCase(), field.getName());
        }
    }

    private void processJoinColumn(Field field, Map<String, String> fieldColumnMap) {
        if (field.isAnnotationPresent(jakarta.persistence.JoinColumn.class)) {
            jakarta.persistence.JoinColumn joinColumn = field.getAnnotation(jakarta.persistence.JoinColumn.class);
            if (!joinColumn.name().isEmpty()) {
                fieldColumnMap.put(joinColumn.name().toLowerCase(), field.getName());
            }
        }
    }

    private void processRelationshipAnnotations(Field field, Map<String, String> fieldColumnMap) {
        if (field.isAnnotationPresent(jakarta.persistence.ManyToOne.class) ||
                field.isAnnotationPresent(jakarta.persistence.OneToOne.class)) {
            fieldColumnMap.put(field.getName().toLowerCase(), field.getName());
        }
    }

    private Specification<T> createSpecification(List<SearchCriteria> criteriaList) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = criteriaList.stream()
                    .map(criteria -> buildPredicate(criteria, root, criteriaBuilder, query))
                    .toList();

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Predicate buildPredicate(SearchCriteria criteria, Root<T> root, CriteriaBuilder criteriaBuilder, CriteriaQuery query) {
        Path<?> field = resolveFieldPath(root, criteria.getKey());
        Class<?> fieldType = field.getJavaType();
        String operation = criteria.getOperation();
        Object value = requiresRawValue(operation)
                ? criteria.getValue()
                : convertToPostgresType(criteria.getValue(), fieldType);

        return createOperationPredicate(criteriaBuilder, field, value, operation, fieldType, query, root);
    }

    private Predicate createOperationPredicate(
            CriteriaBuilder criteriaBuilder,
            Path<?> field,
            Object value,
            String operation,
            Class<?> fieldType,
            CriteriaQuery<?> query,
            Root<T> root) {

        return switch (operation.toLowerCase()) {
            case "gt" -> compare(criteriaBuilder, field, value, ComparisonOperator.GREATER_THAN);
            case "lt" -> compare(criteriaBuilder, field, value, ComparisonOperator.LESS_THAN);
            case "gte" -> compare(criteriaBuilder, field, value, ComparisonOperator.GREATER_THAN_OR_EQUAL);
            case "lte" -> compare(criteriaBuilder, field, value, ComparisonOperator.LESS_THAN_OR_EQUAL);
            case "like" -> createLikePredicate(criteriaBuilder, field, value);
            case "startswith" -> createStartsWithPredicate(criteriaBuilder, field, value);
            case "endswith" -> createEndsWithPredicate(criteriaBuilder, field, value);
            case "in" -> createInPredicate(field, value);
            case "notin" -> createNotInPredicate(criteriaBuilder, field, value);
            case "noteq" -> criteriaBuilder.notEqual(field, value);
            case "between" -> createBetweenPredicate(criteriaBuilder, field, value, fieldType);
            case "notingroup" -> createNotInGroupPredicate(criteriaBuilder, value, query, root);
            default -> criteriaBuilder.equal(field, value);
        };
    }

    private LocalDateTime parseLocalDateTime(Object value) {
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        } else if (value instanceof String) {
            return LocalDateTime.parse(value.toString());
        } else {
            throw new IllegalArgumentException("Invalid value for LocalDateTime: " + value);
        }
    }

    private void validateStringOperation(Class<?> fieldType, String operation) {
        if (fieldType.equals(Boolean.class) ||
                fieldType.equals(Date.class) ||
                fieldType.equals(Timestamp.class) ||
                fieldType.equals(LocalDateTime.class)) {
            log.warn("Applying string operation '{}' to field of type '{}' - this may not produce expected results",
                    operation, fieldType.getSimpleName());
        }
    }

    private Predicate createLikePredicate(CriteriaBuilder criteriaBuilder, Path<?> field, Object value) {
        Class<?> fieldType = field.getJavaType();
        validateStringOperation(fieldType, "like");

        if (fieldType.equals(String.class)) {
            return criteriaBuilder.like(
                    criteriaBuilder.lower(field.as(String.class)),
                    "%" + value.toString().toLowerCase() + "%"
            );
        } else {
            return criteriaBuilder.equal(field, convertToPostgresType(value.toString(), fieldType));
        }
    }

    private Predicate createStartsWithPredicate(CriteriaBuilder criteriaBuilder, Path<?> field, Object value) {
        Class<?> fieldType = field.getJavaType();
        validateStringOperation(fieldType, "startswith");

        if (fieldType.equals(String.class)) {
            return criteriaBuilder.like(
                    criteriaBuilder.lower(field.as(String.class)),
                    value.toString().toLowerCase() + "%"
            );
        } else {
            return criteriaBuilder.equal(field, convertToPostgresType(value.toString(), fieldType));
        }
    }

    private Predicate createEndsWithPredicate(CriteriaBuilder criteriaBuilder, Path<?> field, Object value) {
        Class<?> fieldType = field.getJavaType();
        validateStringOperation(fieldType, "endswith");

        if (fieldType.equals(String.class)) {
            return criteriaBuilder.like(
                    criteriaBuilder.lower(field.as(String.class)),
                    "%" + value.toString().toLowerCase()
            );
        } else {
            return criteriaBuilder.equal(field, convertToPostgresType(value.toString(), fieldType));
        }
    }

    private Predicate createInPredicate(Path<?> field, Object value) {
        List<Object> typedValues = Arrays.stream(value.toString().split(","))
                .map(val -> convertToPostgresType(val.trim(), field.getJavaType()))
                .toList();
        return field.in(typedValues);
    }

    private Predicate createNotInPredicate(CriteriaBuilder criteriaBuilder, Path<?> field, Object value) {
        List<String> values = Arrays.asList(value.toString().split(","));

        List<Object> typedValues = values.stream().map(val -> convertToPostgresType(val.trim(), field.getJavaType())).toList();

        return criteriaBuilder.not(field.in(typedValues));
    }

    private Predicate createNotInGroupPredicate(
            CriteriaBuilder criteriaBuilder,
            Object value,
            CriteriaQuery<?> query,
            Root<T> root
    ) {
        String[] parts = value.toString().split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("notingroup value must be in format: 'relationshipField,groupId'");
        }

        String relationshipField = parts[0];
        String groupId = parts[1];

        Subquery<Long> subquery = query.subquery(Long.class);

        Class<?> joinTableClass = getRelationshipClass(root.getJavaType(), relationshipField);

        Root<?> groupRoot = subquery.from(joinTableClass);
        String inverseField = getInverseRelationshipField(joinTableClass, root.getJavaType());
        Join<?, ?> entityJoin = groupRoot.join(inverseField);

        subquery.select(entityJoin.get("id"))
                .where(criteriaBuilder.equal(groupRoot.get("uuid"), UUID.fromString(groupId)));

        return criteriaBuilder.not(root.get("id").in(subquery));
    }

    private Class<?> getRelationshipClass(Class<?> entityClass, String relationshipField) {
        return relationshipCache
                .computeIfAbsent(entityClass, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(relationshipField, k -> {
                    try {
                        Field field = entityClass.getDeclaredField(k);
                        if (Collection.class.isAssignableFrom(field.getType())) {
                            ParameterizedType type = (ParameterizedType) field.getGenericType();
                            return (Class<?>) type.getActualTypeArguments()[0];
                        }
                        return field.getType();
                    } catch (NoSuchFieldException e) {
                        throw new IllegalArgumentException("Invalid relationship field: " + k, e);
                    }
                });
    }

    private String getInverseRelationshipField(Class<?> entityClass, Class<?> targetClass) {
        RelationshipKey key = new RelationshipKey(entityClass, targetClass);
        return inverseRelationshipCache.computeIfAbsent(key, k ->
                Arrays.stream(k.entityClass().getDeclaredFields())
                        .filter(field -> isMatchingRelationship(field, k.targetClass()))
                        .findFirst()
                        .map(Field::getName)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Could not find inverse relationship from " + k.entityClass().getSimpleName() +
                                        " to " + k.targetClass().getSimpleName())));
    }

    private boolean isMatchingRelationship(Field field, Class<?> targetClass) {
        if (Collection.class.isAssignableFrom(field.getType())) {
            ParameterizedType type = (ParameterizedType) field.getGenericType();
            return type.getActualTypeArguments()[0].equals(targetClass);
        }
        return field.getType().equals(targetClass);
    }

    private Predicate createBetweenPredicate(
            CriteriaBuilder criteriaBuilder,
            Path<?> field,
            Object value,
            Class<?> fieldType) {
        String[] rangeValues = value.toString().split(",");
        validateBetweenValues(rangeValues);

        if (fieldType.equals(LocalDateTime.class)) {
            LocalDateTime startDate = parseLocalDateTime(rangeValues[0].trim());
            LocalDateTime endDate = parseLocalDateTime(rangeValues[1].trim());

            log.debug("Parsed date range - Start: {}, End: {}", startDate, endDate);

            return criteriaBuilder.between(
                    field.as(LocalDateTime.class),
                    startDate,
                    endDate
            );
        }

        Object startValue = convertToPostgresType(rangeValues[0].trim(), fieldType);
        Object endValue = convertToPostgresType(rangeValues[1].trim(), fieldType);

        validateComparableTypes(startValue, endValue);

        return between(criteriaBuilder, field, startValue, endValue);
    }

    private void validateBetweenValues(String[] rangeValues) {
        if (rangeValues.length != 2) {
            throw new IllegalArgumentException(
                    "Invalid range for 'between' operation. Expected format: 'startValue,endValue'"
            );
        }
    }

    private void validateComparableTypes(Object startValue, Object endValue) {
        if (!(startValue instanceof Comparable) || !(endValue instanceof Comparable)) {
            throw new IllegalArgumentException("'between' operation is only supported for comparable types");
        }
    }

    private <Y extends Comparable<? super Y>> Predicate compare(
            CriteriaBuilder criteriaBuilder,
            Path<?> field,
            Object value,
            ComparisonOperator operator
    ) {
        if (!(value instanceof Comparable<?> comparableValue)) {
            throw new IllegalArgumentException("Comparison operations require comparable values");
        }
        if (!Comparable.class.isAssignableFrom(field.getJavaType())) {
            throw new IllegalArgumentException("Field type is not comparable: " + field.getJavaType());
        }

        @SuppressWarnings("unchecked")
        Path<Y> typedField = (Path<Y>) field;
        @SuppressWarnings("unchecked")
        Y typedValue = (Y) comparableValue;

        return switch (operator) {
            case GREATER_THAN -> criteriaBuilder.greaterThan(typedField, typedValue);
            case LESS_THAN -> criteriaBuilder.lessThan(typedField, typedValue);
            case GREATER_THAN_OR_EQUAL -> criteriaBuilder.greaterThanOrEqualTo(typedField, typedValue);
            case LESS_THAN_OR_EQUAL -> criteriaBuilder.lessThanOrEqualTo(typedField, typedValue);
        };
    }

    private <Y extends Comparable<? super Y>> Predicate between(
            CriteriaBuilder criteriaBuilder,
            Path<?> field,
            Object startValue,
            Object endValue
    ) {
        @SuppressWarnings("unchecked")
        Path<Y> typedField = (Path<Y>) field;
        @SuppressWarnings("unchecked")
        Y start = (Y) startValue;
        @SuppressWarnings("unchecked")
        Y end = (Y) endValue;
        return criteriaBuilder.between(typedField, start, end);
    }

    private enum ComparisonOperator {
        GREATER_THAN,
        LESS_THAN,
        GREATER_THAN_OR_EQUAL,
        LESS_THAN_OR_EQUAL
    }

    private Path<?> resolveFieldPath(Root<T> root, String fieldKey) {
        if (fieldKey.contains(".")) {
            return resolveNestedFieldPath(root, fieldKey);
        }
        return root.get(fieldKey);
    }

    private Path<?> resolveNestedFieldPath(Root<T> root, String fieldKey) {
        String[] pathElements = fieldKey.split("\\.");
        Path<?> path = root;
        for (String element : pathElements) {
            path = path.get(element);
        }
        return path;
    }

    private String getColumnAnnotationName(Field field) {
        Column columnAnnotation = field.getAnnotation(Column.class);
        return (columnAnnotation != null && !columnAnnotation.name().isEmpty())
                ? columnAnnotation.name()
                : null;
    }

    private Object convertToPostgresType(Object value, Class<?> fieldType) {
        if (value == null) {
            return null;
        }

        String stringValue = value.toString().trim();
        try {
            return convertValue(stringValue, fieldType);
        } catch (Exception e) {
            log.error("Error converting value '{}' to type '{}': {}",
                    stringValue, fieldType, e.getMessage());
            throw new IllegalArgumentException("Invalid value for field type: " + fieldType, e);
        }
    }

    private Object convertValue(String stringValue, Class<?> fieldType) {
        if (fieldType.equals(UUID.class)) return UUID.fromString(stringValue);
        if (fieldType.equals(Boolean.class)) return convertToBoolean(stringValue);
        if (fieldType.equals(Integer.class)) return Integer.parseInt(stringValue);
        if (fieldType.equals(Long.class)) return Long.parseLong(stringValue);
        if (fieldType.equals(Double.class)) return Double.parseDouble(stringValue);
        if (fieldType.equals(Float.class)) return Float.parseFloat(stringValue);
        if (fieldType.equals(BigDecimal.class)) return new BigDecimal(stringValue);
        if (fieldType.equals(Date.class)) return Date.valueOf(stringValue);
        if (fieldType.equals(Timestamp.class)) return Timestamp.valueOf(stringValue);
        if (fieldType.equals(LocalDateTime.class)) return LocalDateTime.parse(stringValue);
        if (Enum.class.isAssignableFrom(fieldType)) return convertEnumValue(stringValue, fieldType);
        if (fieldType.equals(String.class)) return stringValue;

        log.warn("Unhandled field type: {}", fieldType);
        return stringValue;
    }

    private boolean requiresRawValue(String operation) {
        if (operation == null) {
            return false;
        }
        return switch (operation.toLowerCase(Locale.ROOT)) {
            case "in", "notin", "between", "notingroup" -> true;
            default -> false;
        };
    }

    private Object convertEnumValue(String value, Class<?> fieldType) {
        Class<? extends Enum> enumClass = ((Class<?>) fieldType).asSubclass(Enum.class);
        String normalizedValue = value.trim();

        for (Enum<?> constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(normalizedValue)) {
                return constant;
            }
        }

        try {
            Method fromValueMethod = fieldType.getDeclaredMethod("fromValue", String.class);
            return fromValueMethod.invoke(null, normalizedValue);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException reflectionException) {
            log.error("Unable to convert '{}' to enum {}: {}", normalizedValue, fieldType.getSimpleName(), reflectionException.getMessage());
            throw new IllegalArgumentException(
                    "Invalid enum value '" + normalizedValue + "' for " + fieldType.getSimpleName());
        }
    }

    private boolean convertToBoolean(String value) {
        return value.equalsIgnoreCase("1") || value.equalsIgnoreCase("true");
    }
}
