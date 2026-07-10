package apps.sarafrika.elimika.commerce.payment.service;

import apps.sarafrika.elimika.commerce.payment.dto.MpesaCheckoutResponse;
import apps.sarafrika.elimika.commerce.payment.dto.PaymentStatusResponse;

/**
 * Orchestrates M-Pesa payment for internal commerce orders against the mpesa-service gateway.
 */
public interface OrderPaymentService {

    /**
     * Initiates an M-Pesa STK Push for an order that is awaiting payment and stores the returned
     * checkout request id against the order.
     */
    MpesaCheckoutResponse initiateMpesaPayment(String orderId, String phoneNumber);

    /**
     * Polls the gateway for the order's payment status. On the first observed SUCCESS the order is
     * captured, the platform fee is computed and an {@code OrderCompletedEvent} is published so the
     * read-model updates and earner wallets are credited. Idempotent: once captured, subsequent
     * calls return CAPTURED without re-capturing or re-publishing.
     */
    PaymentStatusResponse getPaymentStatus(String orderId);
}
