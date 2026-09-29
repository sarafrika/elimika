package apps.sarafrika.elimika.search.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;

class SearchPropertiesTest {

    @Test
    void readFlagFromEnvironmentVariableMatchesIndexNameWithUnderscore() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, Map.of(
                "SEARCH_ENABLED", "true",
                "SEARCH_READENABLED_MARKETPLACE_JOBS", "true",
                "SEARCH_READENABLED_COURSES", "true")));
        SearchProperties properties = new Binder(ConfigurationPropertySources.get(environment))
                .bind("search", SearchProperties.class)
                .get();

        assertThat(properties.isReadEnabled("marketplace_jobs")).isTrue();
        assertThat(properties.isReadEnabled("courses")).isTrue();
        assertThat(properties.isReadEnabled("people")).isFalse();
    }

    @Test
    void readFlagsAreOffWhenSearchIsDisabled() {
        SearchProperties properties = new Binder(new MapConfigurationPropertySource(Map.of(
                "search.enabled", "false",
                "search.read-enabled.courses", "true")))
                .bind("search", SearchProperties.class)
                .get();

        assertThat(properties.isReadEnabled("courses")).isFalse();
    }
}
