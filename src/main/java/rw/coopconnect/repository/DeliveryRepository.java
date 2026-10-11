package rw.coopconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rw.coopconnect.domain.Delivery;
import rw.coopconnect.domain.enums.DeliveryStatus;

import java.util.List;
import java.util.Optional;

/**
 * FR-08: Delivery persistence with transporter and status queries.
 */
@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    Optional<Delivery> findByOrderId(Long orderId);

    List<Delivery> findByTransporterId(Long transporterId);

    List<Delivery> findByTransporterIdAndStatus(Long transporterId, DeliveryStatus status);

    List<Delivery> findByStatus(DeliveryStatus status);
}
