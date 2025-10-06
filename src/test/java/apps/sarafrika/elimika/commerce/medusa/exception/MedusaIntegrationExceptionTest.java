package apps.sarafrika.elimika.commerce.medusa.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MedusaIntegrationExceptionTest {

    @Test
    void constructsWithMessage() {
        MedusaIntegrationException exception = new MedusaIntegrationException("failure");
        assertThat(exception).hasMessage("failure");
    }

    @Test
    void constructsWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("root");
        MedusaIntegrationException exception = new MedusaIntegrationException("failure", cause);
        assertThat(exception).hasMessage("failure");
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}
