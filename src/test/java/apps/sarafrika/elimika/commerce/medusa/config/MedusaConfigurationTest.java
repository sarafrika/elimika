package apps.sarafrika.elimika.commerce.medusa.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MedusaConfigurationTest {

    @Test
    void medusaRestClient_setsBaseUrlAndHeaders() {
        MedusaProperties properties = new MedusaProperties();
        properties.setBaseUrl("https://medusa.example.com");
        properties.setApiToken("token");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        MedusaConfiguration configuration = new MedusaConfiguration();
        RestClient restClient = configuration.medusaRestClient(builder, properties);
        server.expect(requestTo("https://medusa.example.com/test"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token"))
                .andExpect(header("Accept", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        restClient.get().uri("/test").retrieve().body(String.class);
        server.verify();
        assertThat(restClient).isNotNull();
    }
}
