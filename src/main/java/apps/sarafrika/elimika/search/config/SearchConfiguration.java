package apps.sarafrika.elimika.search.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Wires the search module.
 * <p>
 * The indexing executor has exactly one thread. Writes to the engine are applied in the order their
 * transactions committed, so a later update to a document can never be overtaken by an earlier one.
 * <p>
 * Declaring an {@code Executor} bean makes Spring Boot back off from its own
 * {@code applicationTaskExecutor}, which would leave every other {@code @Async} method in the
 * application on this single thread. {@code spring.task.execution.mode=force} in
 * {@code application.yaml} keeps Boot's executor - and keeps it the default for {@code @Async} -
 * regardless of this bean.
 */
@Configuration
@EnableConfigurationProperties(SearchProperties.class)
public class SearchConfiguration {

    public static final String INDEX_EXECUTOR = "searchIndexExecutor";

    @Bean(name = INDEX_EXECUTOR)
    @ConditionalOnProperty(prefix = "search", name = "enabled", havingValue = "true")
    public ThreadPoolTaskExecutor searchIndexExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setThreadNamePrefix("search-index-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
