package apps.sarafrika.elimika.wallet.dto;

import static org.assertj.core.api.Assertions.assertThat;

import apps.sarafrika.elimika.wallet.entity.PayoutMethod;
import apps.sarafrika.elimika.wallet.enums.PayoutMethodStatus;
import apps.sarafrika.elimika.wallet.enums.PayoutMethodType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PayoutMethodDTOTest {

    @Test
    void theFullNumberNeverLeavesTheServer() throws Exception {
        PayoutMethod method = new PayoutMethod();
        method.setUuid(UUID.randomUUID());
        method.setMethodType(PayoutMethodType.MPESA);
        method.setMsisdn("254712345678");
        method.setStatus(PayoutMethodStatus.VERIFIED);

        String json = new ObjectMapper().registerModule(new JavaTimeModule())
                .writeValueAsString(PayoutMethodDTO.from(method));

        assertThat(json).contains("\"masked_msisdn\":\"+254 7•• ••• 678\"")
                .doesNotContain("712345678").doesNotContain("712 345 678");
    }
}
