package com.hopngo.market.service.payment;

import com.hopngo.market.entity.Order;
import com.hopngo.market.dto.PaymentIntentResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

/**
 * Payment provider abstraction interface.
 * Supports multiple payment providers like MOCK, STRIPE_TEST, BKASH, NAGAD.
 */
public interface PaymentProvider {
    String MOCK = "MOCK";
    
    /**
     * Creates a payment intent for the given order.
     * 
     * @param order the order to create payment intent for
     * @return payment intent response with client secret and other details
     */
    PaymentIntentResponse createPaymentIntent(Order order);
    
    /**
     * Verifies webhook signature and processes the webhook request.
     * 
     * @param request the HTTP servlet request containing webhook data
     * @return true if webhook is valid and processed successfully, false otherwise
     */
    boolean verifyWebhook(HttpServletRequest request);
    
    /**
     * Verifies webhook signature using raw body and headers.
     * 
     * @param rawBody the raw webhook payload
     * @param headers the HTTP headers containing signature
     * @return true if webhook signature is valid, false otherwise
     */
    boolean verifyWebhookSignature(String rawBody, HttpHeaders headers);
    
    /**
     * Processes a refund for the given payment.
     * 
     * @param paymentId the original payment ID to refund
     * @param refundAmount the amount to refund
     * @param currency the currency of the refund
     * @param reason the reason for the refund
     * @return refund response with provider refund ID and status
     */
    RefundResponse processRefund(String paymentId, java.math.BigDecimal refundAmount, String currency, String reason);
    
    /**
     * Returns the name/identifier of this payment provider.
     * 
     * @return provider name (e.g., "MOCK", "STRIPE_TEST", "BKASH")
     */
    String name();
}