package apps.sarafrika.elimika.notifications.internal;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * {@code supplyAsync} on the common pool, but with the app's class loader. Common-pool threads
 * can't see resources inside the Boot jar, so Thymeleaf templates silently failed to resolve there.
 */
public final class AppClassLoaderAsync {

    private static final ClassLoader APP = AppClassLoaderAsync.class.getClassLoader();

    private AppClassLoaderAsync() {
    }

    public static <T> CompletableFuture<T> supply(Supplier<T> work) {
        return CompletableFuture.supplyAsync(() -> {
            Thread thread = Thread.currentThread();
            ClassLoader previous = thread.getContextClassLoader();
            thread.setContextClassLoader(APP);
            try {
                return work.get();
            } finally {
                thread.setContextClassLoader(previous);
            }
        });
    }
}
