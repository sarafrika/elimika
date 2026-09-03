package apps.sarafrika.elimika.commerce.internal.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Which machine may settle an order that is not its own.
 *
 * <p>The M-Pesa gateway calls {@code /api/v1/commerce/orders/{orderId}/payment-callback} with a
 * Keycloak client-credentials token. That token stands for no learner, so the order guard cannot ask
 * "is this the buyer" — it has to ask "is this the gateway" instead. Without a named client that
 * question degenerates into "is this any service account in the realm", which every internal
 * integration would answer yes to.
 *
 * <p>The default names the gateway's own client. An environment whose gateway authenticates as
 * something else sets {@code COMMERCE_PAYMENT_CALLBACK_CLIENT_IDS}; until it does, the refused
 * client id is logged on every attempt and the reconciliation sweep still settles the payment within
 * the minute, so a wrong name here delays capture rather than losing it.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "commerce.payment.callback")
public class PaymentCallbackProperties {

    /**
     * Keycloak client ids whose service-account token may read and settle an order it does not own.
     */
    private List<String> clientIds = List.of("mpesa-service");
}
