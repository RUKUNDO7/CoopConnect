package rw.coopconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rw.coopconnect.domain.Rating;

import java.util.Optional;

/**
 * FR-10: Rating persistence.
 */
@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);
}
