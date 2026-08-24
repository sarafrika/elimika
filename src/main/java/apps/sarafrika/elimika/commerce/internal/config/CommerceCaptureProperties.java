package apps.sarafrika.elimika.commerce.internal.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * How a completed checkout becomes a paid order.
 *
 * <p>Read in two places that must never disagree: the order service, which decides whether to
 * capture without a gateway, and the payment-mode endpoint, which tells the web client whether to
 * send the learner to the payment page at all. A client that believes payment is required while the
 * server captures on completion (or the reverse) strands the learner on an order nobody will
 * settle, so both sides read this one object rather than repeating the property name.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "commerce.capture")
public class CommerceCaptureProperties {

    /**
     * When true, completing a checkout captures the order immediately with no gateway involved, so
     * no payment is required and the payment page is skipped. In production this is false and
     * capture is driven only by a confirmed M-Pesa payment.
     */
    private boolean autoOnComplete = true;
}
