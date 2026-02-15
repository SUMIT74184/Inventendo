package org.example.billingpayment.Model;

import io.lettuce.core.dynamic.annotation.CommandNaming;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;


    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false,length = 3)
    private String currency;

    @Column(nullable = false)
    private String tenantId;         // Which company/tenant made this payment

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentGateway gateway;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;


    @Column(name = "gateway_payment_id")
    private String gatewayPaymentId;

    @Column(name = "gateway_order_id")
    private String gatewayOrderId;

    @Column(name = "gateway_signature")
    private String gatewaySignature;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_At")
    private LocalDateTime updatedAt;

    @Column(name = "completed_At")
    private LocalDateTime completedAt;

    public enum PaymentGateway{
        STRIPE,
        RAZORPAY,
        PAYPAL
    }
    public enum PaymentStatus {
        INITIATED,
        PENDING,
        SUCCESS,
        FAILED,
        REFUNDED,
        CANCELLED
    }

}

