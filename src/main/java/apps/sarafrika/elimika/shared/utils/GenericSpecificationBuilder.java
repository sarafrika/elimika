package apps.sarafrika.elimika.shared.utils;

import jakarta.persistence.Column;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
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

@Component
@Slf4j
public class GenericSpecificationBuilder<T> {
    private static final List<String> EXCLUDED_PARAMS = List.of("page", "size", "sort");
    private final Map<Class<?>, Map<String, String>> fieldColumnCache = new HashMap<>();
    private final Map<Class<?>, Map<String, Class<?>>> relationshipCache = new ConcurrentHashMap<>();
    private final Map<RelationshipKey, String> inverseRelationshipCache = new ConcurrentHashMap<>();

    private record RelationshipKey(Class<?> entityClass, Class<?> targetClass) {
    }

    public Specification<T> buildSpecification(Class<T> entityClass, Map<String, String> searchParams) {
        Map<String, String> fieldColumnMap = getFieldColumnMap(entityClass);
        List<SearchCriteria> criteriaList = buildSearchCriteria(searchParams, fieldColumnMap);
        return criteriaList.isEmpty() ? null : createSpecification(criteriaList);
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

        criteriaList.add(new SearchCriteria(
                resolvedField != null ? resolvedField : criteriaInfo.fieldName(),
                criteriaInfo.operation(),
                value
        ));
    }

    private record SearchCriteriaInfo(String fieldName, String operation) {
    }

    private SearchCriteriaInfo parseSearchKey(String key) {
        int lastUnderscoreIndex = key.lastIndexOf("_");
        if (lastUnderscoreIndex != -1) {
            return new SearchCriteriaInfo(
                    key.substring(0, lastUnderscoreIndex),
                    key.substring(lastUnderscoreIndex + 1)
            );
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
        Object value = criteria.getOperation().equalsIgnoreCase("notingroup")
                ? criteria.getValue()
                : convertToPostgresType(criteria.getValue(), fieldType);

        return createOperationPredicate(criteriaBuilder, field, value, criteria.getOperation(), fieldType, query, root);
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
            case "gt" -> criteriaBuilder.greaterThan(field.as(Comparable.class), (Comparable) value);
            case "lt" -> criteriaBuilder.lessThan(field.as(Comparable.class), (Comparable) value);
            case "gte" -> criteriaBuilder.greaterThanOrEqualTo(field.as(Comparable.class), (Comparable) value);
            case "lte" -> criteriaBuilder.lessThanOrEqualTo(field.as(Comparable.class), (Comparable) value);
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
        return field.in(Arrays.stream(value.toString().split(",")).toList());
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

        return criteriaBuilder.between(
                field.as(Comparable.class),
                (Comparable) startValue,
                (Comparable) endValue
        );
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
