package apps.sarafrika.elimika.shared.internal;

import apps.sarafrika.elimika.shared.exceptions.DatabaseAuditException;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.persistence.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;

/**
 * This class will automatically run before and after database operations
 * to validate data and provide user-friendly error messages.
 */
@Slf4j
@Component
public class DatabaseAuditListener {

    private static Validator validator;
    private static EntityManager entityManager;

    public static void setValidator(Validator validator) {
        DatabaseAuditListener.validator = validator;
    }

    public static void setEntityManager(EntityManager entityManager) {
        DatabaseAuditListener.entityManager = entityManager;
    }

    @PrePersist
    public void prePersist(Object entity) {
        log.debug("Validating before creating: {}", entity.getClass().getSimpleName());

        try {
            validateEntity(entity);
            checkUniqueConstraints(entity);
            checkForeignKeyConstraints(entity);
        } catch (Exception ex) {
            log.error("Validation failed for: {}", entity.getClass().getSimpleName(), ex);
            throw new DatabaseAuditException("Unable to create " + getEntityName(entity), ex);
        }
    }

    @PreUpdate
    public void preUpdate(Object entity) {
        log.debug("Validating before updating: {}", entity.getClass().getSimpleName());

        try {
            validateEntity(entity);
            checkUniqueConstraints(entity);
            checkForeignKeyConstraints(entity);
        } catch (Exception ex) {
            log.error("Validation failed for: {}", entity.getClass().getSimpleName(), ex);
            throw new DatabaseAuditException("Unable to update " + getEntityName(entity), ex);
        }
    }

    @PostPersist
    public void postPersist(Object entity) {
        log.info("AUDIT: CREATED - {} - ID: {} - UUID: {}",
                entity.getClass().getSimpleName(),
                getEntityId(entity),
                getEntityUuid(entity));
    }

    @PostUpdate
    public void postUpdate(Object entity) {
        log.info("AUDIT: UPDATED - {} - ID: {} - UUID: {}",
                entity.getClass().getSimpleName(),
                getEntityId(entity),
                getEntityUuid(entity));
    }

    @PostRemove
    public void postRemove(Object entity) {
        log.info("AUDIT: DELETED - {} - ID: {} - UUID: {}",
                entity.getClass().getSimpleName(),
                getEntityId(entity),
                getEntityUuid(entity));
    }

    private void validateEntity(Object entity) {
        if (validator == null) return;

        Set<ConstraintViolation<Object>> violations = validator.validate(entity);
        if (!violations.isEmpty()) {
            StringBuilder errors = new StringBuilder();
            for (ConstraintViolation<Object> violation : violations) {
                errors.append(violation.getPropertyPath())
                        .append(": ")
                        .append(violation.getMessage())
                        .append("; ");
            }
            throw new DatabaseAuditException("Invalid data: " + errors.toString());
        }
    }

    private void checkUniqueConstraints(Object entity) {
        if (entityManager == null) return;

        try {
            String entityName = entity.getClass().getSimpleName();
            java.lang.reflect.Field[] fields = entity.getClass().getDeclaredFields();

            for (java.lang.reflect.Field field : fields) {
                if (field.isAnnotationPresent(Column.class)) {
                    Column column = field.getAnnotation(Column.class);
                    if (column.unique()) {
                        checkUniqueField(entity, field, entityName);
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("Could not check unique constraints", ex);
        }
    }

    private void checkForeignKeyConstraints(Object entity) {
        if (entityManager == null) return;

        try {
            java.lang.reflect.Field[] fields = entity.getClass().getDeclaredFields();

            for (java.lang.reflect.Field field : fields) {
                if (field.isAnnotationPresent(ManyToOne.class) ||
                        field.isAnnotationPresent(OneToOne.class)) {

                    field.setAccessible(true);
                    Object referencedEntity = field.get(entity);

                    if (referencedEntity != null) {
                        Object referencedId = getEntityId(referencedEntity);
                        if (referencedId != null) {
                            Object found = entityManager.find(field.getType(), referencedId);
                            if (found == null) {
                                String fieldName = getFieldDisplayName(field.getName());
                                throw new DatabaseAuditException("Referenced " + fieldName + " does not exist");
                            }
                        }
                    }
                }
            }
        } catch (DatabaseAuditException ex) {
            throw ex;
        } catch (Exception ex) {
            log.debug("Could not check foreign key constraints", ex);
        }
    }

    private void checkUniqueField(Object entity, java.lang.reflect.Field field, String entityName) {
        try {
            field.setAccessible(true);
            Object fieldValue = field.get(entity);

            if (fieldValue != null) {
                Object entityId = getEntityId(entity);

                String query = String.format(
                        "SELECT COUNT(e) FROM %s e WHERE e.%s = :fieldValue",
                        entityName, field.getName()
                );

                if (entityId != null) {
                    query += " AND e.id != :entityId";
                }

                TypedQuery<Long> countQuery = entityManager.createQuery(query, Long.class);
                countQuery.setParameter("fieldValue", fieldValue);

                if (entityId != null) {
                    countQuery.setParameter("entityId", entityId);
                }

                Long count = countQuery.getSingleResult();

                if (count > 0) {
                    String fieldDisplayName = getFieldDisplayName(field.getName());
                    throw new DatabaseAuditException(fieldDisplayName + " already exists");
                }
            }
        } catch (DatabaseAuditException ex) {
            throw ex;
        } catch (Exception ex) {
            log.debug("Could not check unique field: {}", field.getName(), ex);
        }
    }

    private Object getEntityId(Object entity) {
        try {
            if (entity instanceof BaseEntity) {
                return ((BaseEntity) entity).getId();
            }
        } catch (Exception ex) {
            log.debug("Could not get entity ID", ex);
        }
        return null;
    }

    private Object getEntityUuid(Object entity) {
        try {
            if (entity instanceof BaseEntity) {
                return ((BaseEntity) entity).getUuid();
            }
        } catch (Exception ex) {
            log.debug("Could not get entity UUID", ex);
        }
        return null;
    }

    private String getEntityName(Object entity) {
        String className = entity.getClass().getSimpleName();
        return className.toLowerCase().replaceAll("([A-Z])", " $1").trim();
    }

    private String getFieldDisplayName(String fieldName) {
        return fieldName.replaceAll("([A-Z])", " $1").trim().toLowerCase();
    }
}