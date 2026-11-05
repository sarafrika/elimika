package apps.sarafrika.elimika.commerce.internal.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MpesaReceiptFilterTest {

    @Test
    void onlyARealMpesaReceiptIsKept() {
        assertThat(InternalOrderServiceImpl.mpesaReceiptOrNull("UK42C120ZR")).isEqualTo("UK42C120ZR");
        assertThat(InternalOrderServiceImpl.mpesaReceiptOrNull("ws_CO_05112026163425649708881885")).isNull();
        assertThat(InternalOrderServiceImpl.mpesaReceiptOrNull(null)).isNull();
        assertThat(InternalOrderServiceImpl.mpesaReceiptOrNull("abc")).isNull();
    }
}
