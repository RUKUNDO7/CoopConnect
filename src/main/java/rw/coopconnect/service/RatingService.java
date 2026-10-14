package rw.coopconnect.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.coopconnect.domain.Order;
import rw.coopconnect.domain.Rating;
import rw.coopconnect.domain.User;
import rw.coopconnect.domain.enums.OrderStatus;
import rw.coopconnect.dto.request.RatingRequest;
import rw.coopconnect.exception.DuplicateResourceException;
import rw.coopconnect.exception.ResourceNotFoundException;
import rw.coopconnect.repository.OrderRepository;
import rw.coopconnect.repository.RatingRepository;

import java.util.Optional;

/**
 * FR-10: Buyer order reviews and ratings.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final OrderRepository orderRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public Rating submitRating(User buyer, RatingRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + request.getOrderId()));

        if (!order.getBuyer().getId().equals(buyer.getId())) {
            throw new IllegalArgumentException("Only the ordering buyer can rate this order.");
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Only delivered orders can be rated. Current status: " + order.getStatus());
        }

        if (ratingRepository.existsByOrderId(order.getId())) {
            throw new DuplicateResourceException("Order has already been rated.");
        }

        Rating rating = Rating.builder()
                .order(order)
                .buyer(buyer)
                .score(request.getScore())
                .comment(request.getComment())
                .build();

        Rating saved = ratingRepository.save(rating);

        auditLogService.logAction(
                buyer,
                "SUBMIT_RATING",
                "Rating",
                saved.getId(),
                "Rated order " + order.getOrderNumber() + " with " + saved.getScore() + " stars"
        );
        log.info("Buyer {} submitted rating for order {}", buyer.getEmail(), order.getOrderNumber());
        return saved;
    }

    public Optional<Rating> getRatingByOrderId(Long orderId) {
        return ratingRepository.findByOrderId(orderId);
    }
}
