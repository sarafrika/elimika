package apps.sarafrika.elimika.shared.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers {@link RemovedTextOperatorInterceptor} on every API path.
 */
@Configuration
public class RemovedTextOperatorWebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RemovedTextOperatorInterceptor()).addPathPatterns("/api/**");
    }
}
