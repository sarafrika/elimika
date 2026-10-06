package apps.sarafrika.elimika.authentication.config;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeyCloakConfig {
    // The admin client falls back to the backend client, which application.yaml always defines,
    // so an environment only needs app.keycloak.admin.* when it uses a separate admin client.
    @Value("${app.keycloak.admin.clientId:${app.keycloak.clientId}}")
    private String clientId;
    @Value("${app.keycloak.admin.clientSecret:${app.keycloak.clientSecret}}")
    private String clientSecret;
    @Value("${app.keycloak.realm}")
    private String realm;
    @Value("${app.keycloak.serverUrl}")
    private String serverUrl;


    @Bean
    public Keycloak keycloak(){
        return KeycloakBuilder.builder()
                .clientSecret(clientSecret)
                .clientId(clientId)
                .grantType("client_credentials")
                .realm(realm)
                .serverUrl(serverUrl)
                .build();
    }

}
