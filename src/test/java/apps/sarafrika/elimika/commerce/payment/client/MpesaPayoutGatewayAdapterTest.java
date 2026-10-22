package apps.sarafrika.elimika.commerce.payment.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import apps.sarafrika.elimika.commerce.payment.config.MpesaGatewayProperties;
import apps.sarafrika.elimika.shared.spi.payout.MobileMoneyPayoutGateway.PayoutOutcome;
import apps.sarafrika.elimika.shared.spi.payout.MobileMoneyPayoutGateway.PayoutState;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MpesaPayoutGatewayAdapterTest {

    private static final UUID SHORTCODE = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private MockRestServiceServer server;
    private MpesaPayoutGatewayAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://mpesa.example");
        server = MockRestServiceServer.bindTo(builder).build();
        MpesaGatewayProperties properties = new MpesaGatewayProperties();
        properties.setBaseUrl("https://mpesa.example");
        properties.setPayoutShortcodeUuid(SHORTCODE);
        adapter = new MpesaPayoutGatewayAdapter(builder.build(), properties);
    }

    @Test
    void submitsWithTheWithdrawalAsClientReferenceAndReadsAcceptance() {
        server.expect(requestTo("https://mpesa.example/api/v1/mpesa/b2c"))
                .andExpect(content().json("""
                        {"shortcode_uuid":"11111111-2222-3333-4444-555555555555","phone_number":"254712345678",
                         "amount":1500,"client_reference":"wd-1"}"""))
                .andRespond(withStatus(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body("""
                        {"success":true,"data":{"uuid":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee","status":"PENDING"}}"""));

        PayoutOutcome outcome = adapter.submit("wd-1", "254712345678", new BigDecimal("1500"), "Elimika withdrawal");

        assertThat(outcome.state()).isEqualTo(PayoutState.PENDING);
        assertThat(outcome.gatewayPayoutUuid()).isEqualTo("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    }

    @Test
    void aPayoutMpesaRefusedComesBackFailedNotThrown() {
        server.expect(requestTo("https://mpesa.example/api/v1/mpesa/b2c"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY).contentType(MediaType.APPLICATION_JSON).body("""
                        {"success":true,"data":{"status":"FAILED","result_desc":"Invalid Initiator Information"}}"""));

        PayoutOutcome outcome = adapter.submit("wd-2", "254712345678", new BigDecimal("1500"), null);

        assertThat(outcome.state()).isEqualTo(PayoutState.FAILED);
        assertThat(outcome.failureReason()).isEqualTo("Invalid Initiator Information");
    }

    @Test
    void anOutageThrowsSoTheSameReferenceIsRetried() {
        server.expect(requestTo("https://mpesa.example/api/v1/mpesa/b2c")).andRespond(withServerError());

        assertThatThrownBy(() -> adapter.submit("wd-3", "254712345678", new BigDecimal("1500"), null))
                .isInstanceOf(MpesaGatewayException.class);
    }

    @Test
    void readsTheResultByReferenceAndTreatsUnknownAsEmpty() {
        server.expect(requestTo("https://mpesa.example/api/v1/mpesa/payouts?client_reference=wd-4"))
                .andRespond(withSuccess("""
                        {"status":"SUCCESS","transaction_id":"TJE4K9L2QX","recipient_name":"JANE W KAMAU","charges_paid":15.00}""",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://mpesa.example/api/v1/mpesa/payouts?client_reference=wd-5"))
                .andRespond(withResourceNotFound());

        PayoutOutcome paid = adapter.status("wd-4").orElseThrow();

        assertThat(paid.state()).isEqualTo(PayoutState.SUCCESS);
        assertThat(paid.receipt()).isEqualTo("TJE4K9L2QX");
        assertThat(paid.recipientName()).isEqualTo("JANE W KAMAU");
        assertThat(adapter.status("wd-5")).isEmpty();
    }
}
