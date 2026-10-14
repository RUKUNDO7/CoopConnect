package rw.coopconnect.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.coopconnect.domain.*;
import rw.coopconnect.domain.enums.OrderStatus;
import rw.coopconnect.exception.InsufficientStockException;
import rw.coopconnect.exception.ResourceNotFoundException;
import rw.coopconnect.repository.CartItemRepository;
import rw.coopconnect.repository.CartRepository;
import rw.coopconnect.repository.OrderRepository;
import rw.coopconnect.repository.ProductListingRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FR-06, FR-12: Order placement with pessimistic locking and order management.
 * NFR-04: Concurrency control prevents overselling produce.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductListingRepository listingRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public Order createOrderFromCart(User buyer) {
        Cart cart = cartRepository.findByBuyerId(buyer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for buyer."));

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot checkout an empty cart.");
        }

        String orderNumber = "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        BigDecimal totalAmount = BigDecimal.ZERO;

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .buyer(buyer)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        // NFR-04: Pessimistic locking on listings to guarantee stock availability
        for (CartItem item : cart.getItems()) {
            ProductListing listing = listingRepository.findByIdWithLock(item.getListing().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Listing not found with ID: " + item.getListing().getId()));

            if (!listing.isActive() || listing.getAvailableQuantity().compareTo(item.getQuantity()) < 0) {
                throw new InsufficientStockException("Insufficient stock for produce: " + listing.getTitle() +
                        ". Available: " + listing.getAvailableQuantity() + " " + listing.getUnit());
            }

            // Deduct reserved stock
            listing.setAvailableQuantity(listing.getAvailableQuantity().subtract(item.getQuantity()));
            if (listing.getAvailableQuantity().compareTo(BigDecimal.ZERO) == 0) {
                listing.setActive(false);
            }
            listingRepository.save(listing);

            BigDecimal subtotal = listing.getPricePerUnit().multiply(item.getQuantity());
            totalAmount = totalAmount.add(subtotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .listing(listing)
                    .quantity(item.getQuantity())
                    .unitPrice(listing.getPricePerUnit())
                    .subtotal(subtotal)
                    .build();

            order.getItems().add(orderItem);
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        // Clear cart after successful checkout
        cartItemRepository.deleteByCartId(cart.getId());

        auditLogService.logAction(
                buyer,
                "CREATE_ORDER",
                "Order",
                savedOrder.getId(),
                "Placed order " + savedOrder.getOrderNumber() + " totaling " + savedOrder.getTotalAmount() + " RWF"
        );
        log.info("Order {} created for buyer {} with total {}", savedOrder.getOrderNumber(), buyer.getEmail(), totalAmount);
        return savedOrder;
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
    }

    public Order getOrderByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));
    }

    public Page<Order> getOrdersByBuyer(Long buyerId, Pageable pageable) {
        return orderRepository.findByBuyerId(buyerId, pageable);
    }

    public Page<Order> getOrdersByCooperative(Long cooperativeId, Pageable pageable) {
        return orderRepository.findByCooperativeId(cooperativeId, pageable);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public void cancelOrder(Long orderId, User user, String reason) {
        Order order = getOrderById(orderId);
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order cannot be cancelled in state: " + order.getStatus());
        }

        // Return stock back to inventory
        for (OrderItem item : order.getItems()) {
            ProductListing listing = listingRepository.findByIdWithLock(item.getListing().getId()).orElse(null);
            if (listing != null) {
                listing.setAvailableQuantity(listing.getAvailableQuantity().add(item.getQuantity()));
                listing.setActive(true);
                listingRepository.save(listing);
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        auditLogService.logAction(
                user,
                "CANCEL_ORDER",
                "Order",
                order.getId(),
                "Cancelled order " + order.getOrderNumber() + ". Reason: " + reason
        );
        log.info("Order {} cancelled by {}", order.getOrderNumber(), user.getEmail());
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = getOrderById(orderId);
        order.setStatus(newStatus);
        orderRepository.save(order);
    }

    public List<Order> getOrdersBetweenDates(LocalDateTime start, LocalDateTime end) {
        return orderRepository.findByCreatedAtBetween(start, end);
    }
}
