package apps.sarafrika.elimika.shared.sms;

import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(SmsProperties.class)
public class SmsConfiguration {

    @Bean
    public SmsSender smsSender(SmsProperties properties, RestClient.Builder builder) {
        if (!"celcom".equalsIgnoreCase(properties.getProvider())) {
            return new LoggingSmsSender(properties.getLog().isRevealContent());
        }
        SmsProperties.Celcom celcom = properties.getCelcom();
        if (!StringUtils.hasText(celcom.getApiKey()) || !StringUtils.hasText(celcom.getPartnerId())
                || !StringUtils.hasText(celcom.getShortcode())) {
            throw new IllegalStateException("sms.provider=celcom needs sms.celcom.api-key, partner-id and shortcode");
        }
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(celcom.getTimeout()).build());
        factory.setReadTimeout(celcom.getTimeout());
        RestClient client = builder.clone().baseUrl(celcom.getBaseUrl()).requestFactory(factory).build();
        return new CelcomSmsSender(client, celcom);
    }
}
