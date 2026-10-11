package rw.coopconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rw.coopconnect.domain.DeliveryStatusUpdate;

import java.util.List;

/**
 * FR-08: Delivery status update history.
 */
@Repository
public interface DeliveryStatusUpdateRepository extends JpaRepository<DeliveryStatusUpdate, Long> {

    List<DeliveryStatusUpdate> findByDeliveryIdOrderByTimestampAsc(Long deliveryId);
}
