package rw.coopconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rw.coopconnect.domain.CartItem;

import java.util.Optional;

/**
 * FR-06: Cart item persistence.
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartIdAndListingId(Long cartId, Long listingId);

    void deleteByCartId(Long cartId);
}
