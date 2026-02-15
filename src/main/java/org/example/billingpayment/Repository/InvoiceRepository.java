package org.example.billingpayment.Repository;

import org.example.billingpayment.Model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findByOrderId(String orderId);

    Optional<Invoice> findByPaymentId(String paymentId);

    List<Invoice> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    List<Invoice> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}
