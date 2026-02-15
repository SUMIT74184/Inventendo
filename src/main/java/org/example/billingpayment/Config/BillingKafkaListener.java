package org.example.billingpayment.Config;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.billingpayment.Repository.PaymentRepository;
import org.example.billingpayment.Service.BillingService;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BillingKafkaListener {

    private final BillingService billingService;
    private final PaymentRepository paymentRepository;

}
