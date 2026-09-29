package apps.sarafrika.elimika.search.internal;

import apps.sarafrika.elimika.search.config.SearchProperties;
import apps.sarafrika.elimika.shared.search.SearchAvailability;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Answers {@link SearchAvailability} from {@code search.*} properties. Always registered, so owning
 * modules can depend on it whether search is on or off; with search off every answer is {@code false}.
 */
@Component
@RequiredArgsConstructor
public class PropertiesSearchAvailability implements SearchAvailability {

    private final SearchProperties properties;

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    public boolean isReadEnabled(String index) {
        return properties.isReadEnabled(index);
    }
}
