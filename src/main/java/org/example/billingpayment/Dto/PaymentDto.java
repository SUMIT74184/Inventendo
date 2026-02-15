package org.example.billingpayment.Dto;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.billingpayment.Model.*;

import java.math.BigDecimal;


public class PaymentDto {


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InitiatePaymentRequest{

        @NotBlank(message = "Order ID is required")
        private String orderId;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        private BigDecimal amount;

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be 3-letter ISO code (INR, USD, EUR)")
        private String currency;

        @NotNull(message = "Payment gateway is required")
        private Payment.PaymentGateway gateway;


        private String description;

    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaymentInitiateResponse{
        private String paymentId;
        private String gatewayOrderId;
        private String clientSecret;
        private String razorpayKeyId;
        private BigDecimal amount;
        private String currency;
        private Payment.PaymentGateway gateway;
        private Payment.PaymentStatus status;

    }

    /**
     * RazorpayVerificationRequest: After user pays on Razorpay's UI,
     * the frontend sends these IDs back to verify the payment was genuine.
     * We MUST verify the signature — otherwise anyone can fake a payment!
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RazorpayVerificationRequest {

        @NotBlank
        private String razorpayOrderId;

        @NotBlank
        private String razorpayPaymentId;

        @NotBlank
        private String razorpaySignature;  // HMAC-SHA256 signature from Razorpay

        @NotBlank
        private String internalPaymentId;  // Our payment ID to update status
    }

    /**
     * RefundRequest: To refund a payment back to customer.
     * partial = true means refund only some amount (e.g., partial cancellation)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundRequest{

        @NotBlank
        private String paymentId;

        @DecimalMin(value = "0.01")
        private BigDecimal refundAmount;

        @NotBlank
        private String reason;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaymentResponse{
        private String id;
        private String orderId;
        private String tenantId;
        private BigDecimal amount;
        private String currency;
        private Payment.PaymentGateway gateway;
        private Payment.PaymentStatus status;
        private String gatewayPaymentId;
        private String paymentMethod;
        private String failureReason;
        private String createdAt;
        private String completedAt;
    }



}
