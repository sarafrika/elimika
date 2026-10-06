package apps.sarafrika.elimika.profile.spi;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;

/** Helpers for the profile DTO records. */
public final class ProfileRecords {

    private ProfileRecords() {
    }

    /** A copy of {@code changes} with every null component taken from {@code base}: a partial update. */
    @SuppressWarnings("unchecked")
    public static <R extends Record> R overlay(R changes, R base) {
        if (changes == null) {
            return base;
        }
        if (base == null) {
            return changes;
        }
        Class<R> type = (Class<R>) changes.getClass();
        RecordComponent[] components = type.getRecordComponents();
        Object[] values = new Object[components.length];
        Class<?>[] types = new Class<?>[components.length];
        try {
            for (int i = 0; i < components.length; i++) {
                Object value = components[i].getAccessor().invoke(changes);
                values[i] = value != null ? value : components[i].getAccessor().invoke(base);
                types[i] = components[i].getType();
            }
            Constructor<R> constructor = type.getDeclaredConstructor(types);
            return constructor.newInstance(values);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot overlay " + type.getSimpleName(), e);
        }
    }
}
