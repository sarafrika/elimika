package apps.sarafrika.elimika.shared.search;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * Tells the indexing machinery which entity changes affect an index.
 * <p>
 * A <em>direct</em> trigger maps a changed entity to the UUID of the document it affects - the
 * entity itself, or a parent whose document embeds it. A <em>fan-out</em> trigger maps it to a key
 * (such as {@code "category:<uuid>"}) that the owning {@link SearchDocumentSource} resolves into
 * document UUIDs after the transaction commits, via {@link SearchDocumentSource#resolveFanOut}.
 * <p>
 * The functions run inside JPA's post-insert/update/delete callbacks, in the middle of a flush. They
 * must only read state already loaded on the entity - no queries, no lazy collections - and may
 * return {@code null} to mean "no document affected".
 */
public record SearchIndexTrigger<E>(
        Class<E> entityClass,
        Function<E, UUID> documentUuid,
        Function<E, String> fanOutKey
) {

    public SearchIndexTrigger {
        Objects.requireNonNull(entityClass, "entityClass");
        if ((documentUuid == null) == (fanOutKey == null)) {
            throw new IllegalArgumentException("A trigger maps to either a document UUID or a fan-out key, not both");
        }
    }

    /** A change to {@code entityClass} re-indexes the document with the returned UUID. */
    public static <E> SearchIndexTrigger<E> direct(Class<E> entityClass, Function<E, UUID> documentUuid) {
        return new SearchIndexTrigger<>(entityClass, Objects.requireNonNull(documentUuid), null);
    }

    /** A change to {@code entityClass} re-indexes every document the returned key resolves to. */
    public static <E> SearchIndexTrigger<E> fanOut(Class<E> entityClass, Function<E, String> fanOutKey) {
        return new SearchIndexTrigger<>(entityClass, null, Objects.requireNonNull(fanOutKey));
    }

    public boolean isFanOut() {
        return fanOutKey != null;
    }

    /** Applies the direct mapping to an entity already known to be an instance of the entity class. */
    public UUID documentUuidOf(Object entity) {
        return documentUuid.apply(entityClass.cast(entity));
    }

    /** Applies the fan-out mapping to an entity already known to be an instance of the entity class. */
    public String fanOutKeyOf(Object entity) {
        return fanOutKey.apply(entityClass.cast(entity));
    }
}
