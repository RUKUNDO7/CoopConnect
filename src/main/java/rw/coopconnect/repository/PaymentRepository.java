package rw.coopconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rw.coopconnect.domain.Payment;
import rw.coopconnect.domain.enums.PaymentStatus;

import java.util.List;
import java.util.Optional;

/**
 * FR-07: Payment persistence with idempotency support via transaction_ref.
 * NFR-04: Unique transaction reference ensures idempotent callbacks.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionRef(String transactionRef);

    List<Payment> findByOrderId(Long orderId);

    List<Payment> findByStatus(PaymentStatus status);

    Optional<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);
}
