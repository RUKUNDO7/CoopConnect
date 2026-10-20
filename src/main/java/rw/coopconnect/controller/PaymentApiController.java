package rw.coopconnect.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rw.coopconnect.service.PaymentService;

import java.util.Map;

/**
 * FR-07: Mobile Money webhook callback endpoints for MTN MoMo and Airtel Money.
 * NFR-04: Idempotent callback processing.
 */
@Slf4j
@RestController
@RequestMapping("/api/payment/callback")
@RequiredArgsConstructor
public class PaymentApiController {

    private final PaymentService paymentService;

    @PostMapping("/{provider}")
    public ResponseEntity<Map<String, Object>> handleCallback(
            @PathVariable("provider") String provider,
            @RequestBody Map<String, Object> payload) {

        log.info("Received {} payment callback: {}", provider, payload);

        String transactionRef = (String) payload.get("transactionRef");
        Boolean success = (Boolean) payload.getOrDefault("success", true);
        String failureReason = (String) payload.get("failureReason");

        if (transactionRef == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "transactionRef is required"));
        }

        try {
            paymentService.processPaymentCallback(transactionRef, Boolean.TRUE.equals(success), failureReason);
            return ResponseEntity.ok(Map.of(
                    "status", "PROCESSED",
                    "transactionRef", transactionRef
            ));
        } catch (Exception ex) {
            log.error("Failed to process payment callback for {}", transactionRef, ex);
            return ResponseEntity.internalServerError().body(Map.of("error", ex.getMessage()));
        }
    }
}
