package apps.sarafrika.elimika.shared.utils;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an entity field as safe to expose through the generic search API.
 * <p>
 * {@link GenericSpecificationBuilder} operates on a default-deny basis: only fields carrying this
 * annotation may be used as a {@code searchParams} filter key or as a {@code sort} property. Any
 * other request key is rejected with an {@link IllegalArgumentException} (HTTP 400) rather than
 * being silently ignored, so that unexposed columns cannot be probed through result counts.
 * <p>
 * Do not annotate monetary amounts, rate cards, phone numbers, guardian details, geolocation,
 * stored document paths, credentials or audit identity columns.
 *
 * @author Wilfred Njuguna
 * @since 2026-09-04
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Filterable {
}
