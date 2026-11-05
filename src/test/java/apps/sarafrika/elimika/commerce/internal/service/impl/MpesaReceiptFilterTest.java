package apps.sarafrika.elimika.commerce.internal.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MpesaReceiptFilterTest {

    @Test
    void onlyARealMpesaReceiptIsKept() {
        assertThat(MpesaReceipts.orNull("UK42C120ZR")).isEqualTo("UK42C120ZR");
        assertThat(MpesaReceipts.orNull("ws_CO_05112026163425649708881885")).isNull();
        assertThat(MpesaReceipts.orNull(null)).isNull();
        assertThat(MpesaReceipts.orNull("abc")).isNull();
    }
}
