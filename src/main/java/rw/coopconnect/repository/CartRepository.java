package rw.coopconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rw.coopconnect.domain.Cart;

import java.util.Optional;

/**
 * FR-06: Cart persistence tied to buyer.
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByBuyerId(Long buyerId);
}
