package apps.sarafrika.elimika.commerce.order.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReceiptLinksTest {

    private final ReceiptLinks links = new ReceiptLinks("test-secret", "https://api.example.com/");

    @Test
    void onlyTheIssuedTokenOpensTheReceipt() {
        String url = links.url("order-1").orElseThrow();
        String token = url.substring(url.indexOf("?t=") + 3);

        assertThat(url).startsWith("https://api.example.com/api/v1/public/receipts/order-1.pdf?t=");
        assertThat(links.verify("order-1", token)).isTrue();
        assertThat(links.verify("order-2", token)).isFalse();
        assertThat(links.verify("order-1", null)).isFalse();
        assertThat(new ReceiptLinks("other-secret", "https://api.example.com").verify("order-1", token)).isFalse();
    }

    @Test
    void noSecretMeansNoLink() {
        assertThat(new ReceiptLinks("", "https://api.example.com").url("order-1")).isEmpty();
    }
}
