package org.example.billingpayment.Service;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.billingpayment.Dto.PaymentDto;
import org.example.billingpayment.Model.Payment;
import org.example.billingpayment.Repository.InvoiceRepository;
import org.example.billingpayment.Repository.PaymentRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * BillingService: The orchestrator that ties together Invoice and Payment logic.

 * It decides:
 * - Which payment gateway to use (RAZORPAY for INR, STRIPE for others)
 * - When to generate invoices
 * - When to notify other services via Kafka
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BillingService {

    private final RazorpayPaymentService razorpayPaymentService;
    private final StripePaymentService stripePaymentService;
    private InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String,String>kafkaTemplate;


    private final AtomicLong invoiceCounter = new AtomicLong(1);


    public PaymentDto.PaymentInitiateResponse initiatePayment(
            PaymentDto.InitiatePaymentRequest request, String userId, String tenantId){


        Payment.PaymentGateway gateway = request.getGateway() != null
                ? request.getGateway()
                : inferGateway(request.getCurrency());

        request = PaymentDto.InitiatePaymentRequest.builder()
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .gateway(gateway)
                .description(request.getDescription())
                .description(request.getDescription())
                .build();

        return switch(gateway){
            case  RAZORPAY -> razorpayPaymentService.initiateResponse(request,userId,tenantId);
            case STRIPE -> stripePaymentService.initiateResponse(request,userId,tenantId);
            default -> throw new IllegalArgumentException("Unsupported gateway" + gateway);
        };
    }

    /**
     * After Razorpay verification succeeds → update payment status → evict stale cache
     * → generate invoice → publish Kafka event so Order Service can ship the goods.
     */

    @Transactional
    @CacheEvict(value = "payments", key = "#request.internalPaymentId")
    public PaymentDto.PaymentResponse verifyRazorPayPayment(PaymentDto.RazorpayVerificationRequest request){
        PaymentDto.PaymentResponse paymentResponse = razorpayPaymentService.verifyAndCapturePayment(request);

        if(paymentResponse.getStatus() == Payment.PaymentStatus.SUCCESS){
            generateInvoiceForPayment(paymentResponse);

            //publish to kafka: Order Service listens to "Payment.SUCCESS" to trigger warehouse picking and shipping
            publishPaymentEvent("payment.success",paymentResponse);

        }
        return paymentResponse;


    }

    public Payment getPaymentById(String paymentId){

    }





}
