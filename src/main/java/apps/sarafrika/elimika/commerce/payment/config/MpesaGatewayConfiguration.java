package apps.sarafrika.elimika.commerce.payment.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Registers the {@link RestClient} used to talk to the mpesa-service gateway, pre-configured
 * with the base URL and HTTP Basic credentials from {@link MpesaGatewayProperties}.
 */
@Configuration
@EnableConfigurationProperties(MpesaGatewayProperties.class)
public class MpesaGatewayConfiguration {

    @Bean
    public RestClient mpesaRestClient(RestClient.Builder builder, MpesaGatewayProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(15));

        RestClient.Builder configured = builder.requestFactory(requestFactory);
        if (StringUtils.hasText(properties.getBaseUrl())) {
            configured = configured.baseUrl(properties.getBaseUrl());
        }
        if (StringUtils.hasText(properties.getUsername())) {
            configured = configured.requestInterceptor(
                    new BasicAuthenticationInterceptor(properties.getUsername(), properties.getPassword()));
        }
        return configured.build();
    }
}
