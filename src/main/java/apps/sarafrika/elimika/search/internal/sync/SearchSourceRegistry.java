package apps.sarafrika.elimika.search.internal.sync;

import apps.sarafrika.elimika.shared.search.SearchDocument;
import apps.sarafrika.elimika.shared.search.SearchDocumentSource;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Every {@link SearchDocumentSource} in the application, by index name. Resolved on first use rather
 * than at construction, and fails fast when two sources claim the same index.
 */
@Component
@ConditionalOnProperty(prefix = "search", name = "enabled", havingValue = "true")
public class SearchSourceRegistry {

    /** {@code apps.sarafrika.elimika} - the parent of this module's own package. */
    private static final String APPLICATION_PACKAGE = parentPackage(
            parentPackage(parentPackage(SearchSourceRegistry.class.getPackageName())));

    private final ObjectProvider<SearchDocumentSource<?>> provider;
    private volatile Map<String, SearchDocumentSource<SearchDocument>> sources;

    public SearchSourceRegistry(ObjectProvider<SearchDocumentSource<?>> provider) {
        this.provider = provider;
    }

    public Collection<SearchDocumentSource<SearchDocument>> all() {
        return sources().values();
    }

    public Optional<SearchDocumentSource<SearchDocument>> find(String index) {
        return Optional.ofNullable(sources().get(index));
    }

    /** The application module a source belongs to, from its package: {@code course} for {@code apps.sarafrika.elimika.course.search.X}. */
    public static String moduleOf(SearchDocumentSource<?> source) {
        String packageName = AopUtils.getTargetClass(source).getPackageName();
        String prefix = APPLICATION_PACKAGE + ".";
        if (!packageName.startsWith(prefix)) {
            return packageName;
        }
        String rest = packageName.substring(prefix.length());
        int dot = rest.indexOf('.');
        return (dot < 0 ? rest : rest.substring(0, dot)).toLowerCase(Locale.ROOT);
    }

    public List<SearchDocumentSource<SearchDocument>> ofModule(String module) {
        String wanted = module.trim().toLowerCase(Locale.ROOT);
        return all().stream().filter(source -> moduleOf(source).equals(wanted)).toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, SearchDocumentSource<SearchDocument>> sources() {
        Map<String, SearchDocumentSource<SearchDocument>> resolved = sources;
        if (resolved != null) {
            return resolved;
        }
        synchronized (this) {
            if (sources == null) {
                Map<String, SearchDocumentSource<SearchDocument>> byIndex = new LinkedHashMap<>();
                provider.orderedStream().forEach(source -> {
                    String index = source.definition().name();
                    SearchDocumentSource<SearchDocument> previous =
                            byIndex.putIfAbsent(index, (SearchDocumentSource<SearchDocument>) source);
                    if (previous != null) {
                        throw new IllegalStateException("Two search document sources define index '" + index + "': "
                                + AopUtils.getTargetClass(previous).getName() + " and "
                                + AopUtils.getTargetClass(source).getName());
                    }
                });
                sources = Collections.unmodifiableMap(byIndex);
            }
            return sources;
        }
    }

    private static String parentPackage(String packageName) {
        int dot = packageName.lastIndexOf('.');
        return dot < 0 ? "" : packageName.substring(0, dot);
    }
}
