package org.example.billingpayment.Service;

//import com.razorpay.Payment;
import io.jsonwebtoken.security.MacAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.billingpayment.Model.Payment;
import org.example.billingpayment.Repository.PaymentRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.example.billingpayment.Dto.PaymentDto;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * RazorpayPaymentService: Handles all Razorpay-specific payment logic.

 * Why Razorpay for Indian market?
 * - Supports UPI (most popular payment in India)
 * - Supports all Indian banks for NetBanking
 * - Supports Paytm, PhonePe, GooglePay wallets
 * - Works with INR (no currency conversion needed)
 * - Dashboard in English + Hindi


 * Flow for Razorpay:
 * 1. Backend creates a Razorpay Order (with amount and metadata)
 * 2. Frontend receives the order ID and opens Razorpay checkout modal
 * 3. User pays through any UPI/card/wallet option
 * 4. Razorpay sends payment_id + signature to frontend on success
 * 5. Frontend sends these to OUR backend for verification
 * 6. We verify the signature (HMAC-SHA256) to confirm it's genuine
 * 7. If verified → mark payment as SUCCESS in our DB
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class RazorpayPaymentService {

    @Value("${payment.razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${payment.razorpay.key-secret}")
    private String razorpayKeySecret;


    private final PaymentRepository paymentRepository;


    /**
     * Step 1: Create a Razorpay Order and return details to frontend.

     * IMPORTANT: Razorpay amounts are in PAISE (smallest unit of INR).
     * ₹100 = 10000 paise. You MUST multiply by 100!
     * (Stripe works similarly — USD uses cents, so $1 = 100 cents)
     */

    public PaymentDto.PaymentInitiateResponse initiateResponse(PaymentDto.InitiatePaymentRequest request, String userId, String tenantId) {
        try {
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            long amountInPaise = request.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();


            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", request.getCurrency().toUpperCase()); //INR
            orderRequest.put("receipt", request.getOrderId());  //your order reference
            orderRequest.put("payment_capture", 1); //Auto-capture payment


            //Optional metadata (shows on razorpay dashboard for your reference)
            JSONObject notes = new JSONObject();
            notes.put("orderId", request.getOrderId());
            notes.put("tenantId", tenantId);
            orderRequest.put("notes", notes);

            Order razorpayOrder = razorpay.orders.create(orderRequest);
            String gatewayOrderId = razorpayOrder.get("id");  //"order_xyz12432"

            // Save payment record in our DB with INITIATED status
            Payment payment = Payment.builder()
                    .orderId(request.getOrderId())
                    .tenantId(tenantId)
                    .userId(userId)
                    .amount(request.getAmount())
                    .currency(request.getCurrency().toUpperCase())
                    .gateway(Payment.PaymentGateway.RAZORPAY)
                    .status(Payment.PaymentStatus.INITIATED)
                    .gatewayOrderId(gatewayOrderId)
                    .build();

            Payment saved = paymentRepository.save(payment);
            log.info("Razorpay order created: {} for our payment: {}", gatewayOrderId, saved.getId());

            return PaymentDto.PaymentInitiateResponse.builder()
                    .paymentId(saved.getId())
                    .gatewayOrderId(gatewayOrderId)
                    .razorpayKeyId(razorpayKeyId)
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .gateway(Payment.PaymentGateway.RAZORPAY)
                    .status(Payment.PaymentStatus.INITIATED)
                    .build();

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order: {}", e.getMessage());
            throw new RuntimeException("Payment initiation failed: " + e.getMessage());
        }

    }

    /**
     * Step 5-7: Verify payment after user pays on Razorpay's UI.

     * CRITICAL SECURITY: Always verify the signature!
     * The signature is HMAC-SHA256(orderId + "|" + paymentId, secretKey)
     * If signature doesn't match → payment is fake, reject it!

     * This is the most important security step in the entire payment flow.
     */

    public PaymentDto.PaymentResponse verifyAndCapturePayment(PaymentDto.RazorpayVerificationRequest request){
    Payment payment = paymentRepository.findById(request.getInternalPaymentId())
            .orElseThrow(()-> new RuntimeException("Payment not found"));


    boolean isSignatureValid = verifyRazorpaySignature(
            request.getRazorpayOrderId(),
            request.getRazorpayPaymentId(),
            request.getRazorpaySignature()
    );

    if(!isSignatureValid){
        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setFailureReason("Invalid payment signature - possible fraud attempt");
        paymentRepository.save(payment);
        log.warn("Invalid Razorpay signature for payment: {}", payment.getId());
        throw new SecurityException(("Payment verification failed - invalid signature"));
    }

    payment.setStatus(Payment.PaymentStatus.SUCCESS);
    payment.setGatewayPaymentId(request.getRazorpayPaymentId());
    payment.setGatewaySignature(request.getRazorpaySignature());
    payment.setCompletedAt(java.time.LocalDateTime.now());

    Payment saved = paymentRepository.save(payment);
    log.info("Razorpay payment verified and captured: {}",saved.getId());

    return mapToPaymentResponse(saved);

    }

    private boolean verifyRazorpaySignature(String orderId,String paymentId,String signature){
        try{
            String message = orderId + "|" + paymentId;

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    razorpayKeySecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));


            // Convert byte array to hex String

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash){
                String hex = Integer.toHexString(0xff & b);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString().equals(signature);

        }catch (Exception e){
            log.error("Error verifying Razorpay signature: {}",e.getMessage());
            return false;
        }



    }

    /**
     * Process a refund back to customer through Razorpay.
     * Full refund or partial refund based on refundAmount.
     */
    public PaymentDto.PaymentResponse processRefund(String paymentId,BigDecimal refundAmount){
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(()-> new RuntimeException("Payment not found"));

        if(payment.getStatus()!= Payment.PaymentStatus.SUCCESS){
            throw new IllegalStateException("Can only refund successful payments");
        }
        try{
            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId,razorpayKeySecret);

            JSONObject refundRequest = new JSONObject();

            if(refundAmount!=null){
                //Partial refund - convert to paise
                refundRequest.put("amount",refundAmount.multiply(BigDecimal.valueOf(100)).longValue());
            }

            razorpay.payments.refund(payment.getGatewayPaymentId(),refundRequest);

            payment.setStatus(Payment.PaymentStatus.REFUNDED);
            paymentRepository.save(payment);

            log.info("Refund processed for payment: {}", paymentId);
            return mapToPaymentResponse(payment);

        } catch (RazorpayException e){
            log.error("Razorpay refund failed: {}",e.getMessage());
            throw new RuntimeException("Refund failed: " + e.getMessage());
        }
    }

    private PaymentDto.PaymentResponse mapToPaymentResponse(Payment payment){
         return PaymentDto.PaymentResponse.builder()
                 .id(payment.getId())
                 .orderId(payment.getOrderId())
                 .tenantId(payment.getTenantId())
                 .amount(payment.getAmount())
                 .currency(payment.getCurrency())
                 .status(payment.getStatus())
                 .gatewayPaymentId(payment.getGatewayPaymentId())
                 .paymentMethod(payment.getPaymentMethod())
                 .failureReason(payment.getFailureReason())
                 .createdAt(payment.getCreatedAt()!=null ?  payment.getCreatedAt().toString():null)
                 .completedAt(payment.getCompletedAt()!=null ? payment.getCompletedAt().toString():null)
                 .build();
    }



    }
