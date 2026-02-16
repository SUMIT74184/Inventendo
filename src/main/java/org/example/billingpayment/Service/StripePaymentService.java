package org.example.billingpayment.Service;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.billingpayment.Dto.PaymentDto;
import org.example.billingpayment.Model.Payment;
import org.example.billingpayment.Repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * StripePaymentService: Handles all Stripe-specific payment logic.

 * Why Stripe for international payments?
 * - Supports 135+ currencies
 * - Works globally (US, Europe, Asia, etc.)
 * - Best documentation in the industry
 * - Stripe's PaymentIntent is the modern, recommended approach
 * - Handles 3D Secure (required in EU by PSD2 regulation) automatically

 * Stripe PaymentIntent Flow:
 * 1. Backend creates PaymentIntent → gets a client_secret
 * 2. Frontend uses client_secret with Stripe.js to show payment form
 * 3. User enters card details — Stripe handles 3DS if needed
 * 4. On success, Stripe sends webhook to our backend (or frontend confirms)
 * 5. We update payment status to SUCCESS
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StripePaymentService {

    @Value("${payment.stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${payment.stripe.webhook-secret}")
    private String stripeWebhookSecret;

    private final PaymentRepository paymentRepository;


    public PaymentDto.PaymentInitiateResponse initiateResponse(PaymentDto.InitiatePaymentRequest request,String userId,String tenantId){
        try{
            // Set Stripe API key (ideally do this once in a @PostConstruct or config)
            Stripe.apiKey = stripeSecretKey;

            long amountInSmallestUnit = request.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInSmallestUnit)
                    .setCurrency(request.getCurrency().toLowerCase())
                    .setDescription(request.getDescription())

                    /**
                     * MANUAL capture mode: Authorize card first, capture later.
                     * Useful for inventory check before charging.
                     * AUTOMATIC = charge immediately (simpler, use this for most cases)
                     */
                    .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.AUTOMATIC)
                    .putMetadata("orderId", request.getOrderId())
                    .putMetadata("tenantId", tenantId)
                    .putMetadata("userId", userId)
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            Payment payment = Payment.builder()
                    .orderId(request.getOrderId())
                    .tenantId(tenantId)
                    .userId(userId)
                    .amount(request.getAmount())
                    .currency(request.getCurrency().toUpperCase())
                    .gateway(Payment.PaymentGateway.STRIPE)
                    .status(Payment.PaymentStatus.INITIATED)
                    .gatewayPaymentId(paymentIntent.getId())
                    .build();


            Payment saved = paymentRepository.save(payment);
            log.info("Stripe PaymentIntent created: {} for payment: {}", paymentIntent.getId(), saved.getId());


            return PaymentDto.PaymentInitiateResponse.builder()
                    .paymentId(saved.getId())
                    .gatewayOrderId(paymentIntent.getId())
                    .clientSecret(paymentIntent.getClientSecret()) //Frontend needs this!
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .gateway(Payment.PaymentGateway.STRIPE)
                    .status(Payment.PaymentStatus.INITIATED)
                    .build();

        }catch (StripeException e) {
            log.error("Stripe PaymentIntent creation failed: {}", e.getMessage());
            throw new RuntimeException("Payment initiation failed: " + e.getMessage());
        }

    }
    /**
     * Stripe Webhook Handler: Stripe calls THIS endpoint after payment completes/fails.
     * This is the MOST RELIABLE way to update payment status.
     * (Don't rely only on frontend callbacks — users can close browser mid-payment)

     * Webhook verification: We verify the Stripe-Signature header to ensure
     * the request actually came from Stripe, not an attacker.
     */

    public void handleWebhook(String payload,String stripeSignatureHeader){
        try{
            Stripe.apiKey = stripeSecretKey;
            /**
             * constructEvent() verifies the webhook signature using our webhook secret.
             * This is CRITICAL — without verification, anyone could fake payment success!
             */
            Event event = Webhook.constructEvent(
                    payload,stripeSignatureHeader,stripeWebhookSecret
            );

            log.info("Received Stripe webhook event: {}",event.getType());

            //process different event types
            switch (event.getType()){
                case "payment_intent.succeeded"->{
                    PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                            .getObject().orElseThrow();
                    handlePaymentSuccess(paymentIntent);
                }

                case "payment_intent.payment_failed" ->{
                    PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                            .getObject().orElseThrow();
                    handlePaymentFailure(paymentIntent);
                }

                default -> log.debug("Unhandled Stripe event type: {}",event.getType());
            }
        }catch(SignatureVerificationException e) {
            log.error("Invalid Stripe webhook signature: {}", e.getMessage());
            throw new SecurityException("Invalid webhook signature");

        } catch (Exception e) {
            log.error("Error processing Stripe webhook: {}", e.getMessage());
            throw new RuntimeException("Webhook processing failed");
        }
        }


        private void handlePaymentSuccess(PaymentIntent paymentIntent){
        paymentRepository.findByGatewayPaymentId(paymentIntent.getId())
                .ifPresent(payment -> {
                    payment.setStatus(Payment.PaymentStatus.SUCCESS);
                    payment.setCompletedAt(LocalDateTime.now());
                    payment.setPaymentMethod(payment.getPaymentMethod());
                    paymentRepository.save(payment);
                    log.info("Payment marked SUCCESS: {}",payment.getId());

                });
        }

        private void handlePaymentFailure(PaymentIntent paymentIntent){
        paymentRepository.findByGatewayPaymentId(paymentIntent.getId())
                .ifPresent(payment -> {
                    payment.setStatus(Payment.PaymentStatus.SUCCESS);
                    payment.setFailureReason(
                            paymentIntent.getLastPaymentError()!=null
                            ?
                            paymentIntent.getLastPaymentError().getMessage()
                            : "Payment failed"
                    );
                });
        }


    /**
     * Refund a Stripe payment.
     * If refundAmount is null → full refund.
     * If refundAmount is provided → partial refund.
     */

    public PaymentDto.PaymentResponse processRefund(String paymentId, BigDecimal refundAmount){
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(()-> new RuntimeException("Payment not found"));

        if(payment.getStatus()!= Payment.PaymentStatus.SUCCESS){
            throw new IllegalStateException("Can only refund successful payments");
        }

        try{

            Stripe.apiKey = stripeSecretKey;

            RefundCreateParams.Builder refundParams = RefundCreateParams.builder()
                    .setPaymentIntent(payment.getGatewayPaymentId());

            if (refundAmount != null) {
                refundParams.setAmount(refundAmount.multiply(BigDecimal.valueOf(100)).longValue());
            }

            Refund.create(refundParams.build());

            payment.setStatus(Payment.PaymentStatus.REFUNDED);
            paymentRepository.save(payment);


            log.info("Stripe refund processed for payment: {}",paymentId);

            return PaymentDto.PaymentResponse.builder()
                    .id(payment.getId())
                    .orderId(payment.getOrderId())
                    .status(Payment.PaymentStatus.REFUNDED)
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .gateway(Payment.PaymentGateway.STRIPE)
                    .build();


        }catch (StripeException e) {
            log.error("Stripe refund failed: {}", e.getMessage());
            throw new RuntimeException("Refund failed: " + e.getMessage());
        }
    }


    }






