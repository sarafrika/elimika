package apps.sarafrika.elimika.shared.sms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CelcomSmsSenderTest {

    private MockRestServiceServer server;
    private CelcomSmsSender sender;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://isms.example");
        server = MockRestServiceServer.bindTo(builder).build();
        SmsProperties.Celcom celcom = new SmsProperties.Celcom();
        celcom.setApiKey("key");
        celcom.setPartnerId("42");
        celcom.setShortcode("ELIMIKA");
        sender = new CelcomSmsSender(builder.build(), celcom);
    }

    @Test
    void postsTheDocumentedFieldsAndReadsTheMisspeltSuccessCode() {
        server.expect(requestTo("https://isms.example/api/services/sendsms/"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"apikey":"key","partnerID":"42","shortcode":"ELIMIKA","mobile":"254712345678","message":"Hi"}"""))
                .andRespond(withSuccess("""
                        {"responses":[{"respose-code":200,"response-description":"Success",
                        "mobile":254712345678,"messageid":8290842,"networkid":"1"}]}""", MediaType.APPLICATION_JSON));

        SmsDelivery delivery = sender.send("254712345678", "Hi");

        assertThat(delivery.accepted()).isTrue();
        assertThat(delivery.providerMessageId()).isEqualTo("8290842");
    }

    @Test
    void anyOtherCodeIsARefusal() {
        server.expect(requestTo("https://isms.example/api/services/sendsms/"))
                .andRespond(withSuccess("""
                        {"responses":[{"response-code":1001,"response-description":"Invalid sender id"}]}""",
                        MediaType.APPLICATION_JSON));

        SmsDelivery delivery = sender.send("254712345678", "Hi");

        assertThat(delivery.accepted()).isFalse();
        assertThat(delivery.failure()).contains("1001");
    }

    @Test
    void aProviderOutageIsARefusalNotAnException() {
        server.expect(requestTo("https://isms.example/api/services/sendsms/")).andRespond(withServerError());

        assertThat(sender.send("254712345678", "Hi").accepted()).isFalse();
    }
}
