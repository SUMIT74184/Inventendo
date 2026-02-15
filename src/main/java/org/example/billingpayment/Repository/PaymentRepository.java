package org.example.billingpayment.Repository;

import org.example.billingpayment.Model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    // Find by Stripe PaymentIntent ID or Razorpay payment ID
    Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId);

    // Find by the Razorpay/Stripe order ID (for webhook handling)
    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);

    // All payments for a tenant, newest first
    List<Payment> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    // Payments for a specific order (usually should be 1, but retries create multiple)
    List<Payment> findByOrderIdAndStatus(String orderId, Payment.PaymentStatus status);

    List<Payment> findByOrderId(String orderId);
}
