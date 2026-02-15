package org.example.billingpayment.Model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "invoices")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false,unique = true)
    private String invoiceNumber;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String customerId;

    @Column(precision=10, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;


    private LocalDateTime dueDate;

    private String paymentId;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "invoice_items", joinColumns = @JoinColumn(name = "invoice_id"))
    private List<InvoiceItem> items;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum InvoiceStatus {
        DRAFT,       // Being prepared
        SENT,        // Sent to customer
        PAID,        // Payment received
        OVERDUE,     // Past due date, still unpaid
        CANCELLED    // Invoice voided
    }


    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceItem{
        private String productId;
        private String productName;
        private int quantity;


        @Column(precision = 10, scale = 2)
        private BigDecimal unitPrice;

        @Column(precision = 10, scale = 2)
        private BigDecimal totalPrice;


    }








}
