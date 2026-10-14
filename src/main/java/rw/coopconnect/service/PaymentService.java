package rw.coopconnect.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.coopconnect.domain.Order;
import rw.coopconnect.domain.Payment;
import rw.coopconnect.domain.User;
import rw.coopconnect.domain.enums.OrderStatus;
import rw.coopconnect.domain.enums.PaymentStatus;
import rw.coopconnect.dto.request.CheckoutRequest;
import rw.coopconnect.exception.PaymentException;
import rw.coopconnect.exception.ResourceNotFoundException;
import rw.coopconnect.repository.OrderRepository;
import rw.coopconnect.repository.PaymentRepository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FR-07: Payment processing for MTN Mobile Money and Airtel Money.
 * NFR-04: Idempotent callback processing via unique transaction references.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Transactional
    public Payment initiatePayment(Long orderId, User buyer, CheckoutRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new PaymentException("Payment can only be initiated for PENDING orders. Current status: " + order.getStatus());
        }

        String transactionRef = "PAY-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Payment payment = Payment.builder()
                .order(order)
                .transactionRef(transactionRef)
                .provider(request.getProvider())
                .phoneNumber(request.getPhoneNumber())
                .amount(order.getTotalAmount())
                .status(PaymentStatus.PENDING)
                .initiatedAt(LocalDateTime.now())
                .build();

        Payment saved = paymentRepository.save(payment);

        auditLogService.logAction(
                buyer,
                "INITIATE_PAYMENT",
                "Payment",
                saved.getId(),
                "Initiated " + saved.getProvider() + " payment of " + saved.getAmount() + " RWF for order " + order.getOrderNumber()
        );
        log.info("Payment initiated: Ref {} for Order {}", transactionRef, order.getOrderNumber());
        return saved;
    }

    /**
     * NFR-04: Idempotent callback processing.
     * If the payment is already COMPLETED, simply returns without re-executing business logic.
     */
    @Transactional
    public Payment processPaymentCallback(String transactionRef, boolean success, String failureReason) {
        Payment payment = paymentRepository.findByTransactionRef(transactionRef)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with reference: " + transactionRef));

        // Idempotency check: Already processed
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.warn("Payment callback already processed for transactionRef: {}", transactionRef);
            return payment;
        }

        if (success) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setCompletedAt(LocalDateTime.now());
            payment.setFailureReason(null);

            Order order = payment.getOrder();
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);

            notificationService.notifyUser(
                    order.getBuyer(),
                    "Payment Successful",
                    "Your payment of " + payment.getAmount() + " RWF for order " + order.getOrderNumber() + " was confirmed.",
                    "/orders/" + order.getId()
            );

            auditLogService.logAction(
                    order.getBuyer(),
                    "PAYMENT_SUCCESS",
                    "Payment",
                    payment.getId(),
                    "Payment confirmed for order " + order.getOrderNumber() + " (" + payment.getAmount() + " RWF)"
            );
            log.info("Payment confirmed successfully for order {}", order.getOrderNumber());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setCompletedAt(LocalDateTime.now());
            payment.setFailureReason(failureReason != null ? failureReason : "Payment declined by provider.");

            auditLogService.logAction(
                    payment.getOrder().getBuyer(),
                    "PAYMENT_FAILED",
                    "Payment",
                    payment.getId(),
                    "Payment failed for order " + payment.getOrder().getOrderNumber() + ": " + payment.getFailureReason()
            );
            log.warn("Payment failed for reference {}: {}", transactionRef, failureReason);
        }

        return paymentRepository.save(payment);
    }

    /**
     * Helper for instant simulation/confirmation in development and demo environments.
     */
    @Transactional
    public Payment completePaymentImmediately(Long paymentId, User buyer) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));
        return processPaymentCallback(payment.getTransactionRef(), true, null);
    }
}
