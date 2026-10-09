package rw.coopconnect.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rw.coopconnect.domain.Order;
import rw.coopconnect.domain.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * FR-06, FR-12: Order persistence with buyer, status, and reporting queries.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByBuyerId(Long buyerId, Pageable pageable);

    List<Order> findByBuyerIdAndStatus(Long buyerId, OrderStatus status);

    List<Order> findByStatus(OrderStatus status);

    // FR-12: Orders within a date range for reports
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :start AND :end")
    List<Order> findByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // FR-12: Count orders by status
    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countByStatus();

    // FR-12: Orders involving a cooperative's listings
    @Query("SELECT DISTINCT o FROM Order o " +
           "JOIN o.items oi " +
           "JOIN oi.listing pl " +
           "WHERE pl.cooperative.id = :cooperativeId")
    Page<Order> findByCooperativeId(@Param("cooperativeId") Long cooperativeId, Pageable pageable);
}
