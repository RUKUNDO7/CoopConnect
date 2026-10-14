package rw.coopconnect.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.coopconnect.domain.Delivery;
import rw.coopconnect.domain.DeliveryStatusUpdate;
import rw.coopconnect.domain.Order;
import rw.coopconnect.domain.User;
import rw.coopconnect.domain.enums.DeliveryStatus;
import rw.coopconnect.domain.enums.OrderStatus;
import rw.coopconnect.domain.enums.Role;
import rw.coopconnect.dto.request.DeliveryUpdateRequest;
import rw.coopconnect.exception.ResourceNotFoundException;
import rw.coopconnect.repository.DeliveryRepository;
import rw.coopconnect.repository.DeliveryStatusUpdateRepository;
import rw.coopconnect.repository.OrderRepository;
import rw.coopconnect.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * FR-08: Delivery assignment, dispatch, and tracking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryStatusUpdateRepository statusUpdateRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Transactional
    public Delivery assignTransporter(Long orderId, Long transporterId, User assignedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        User transporter = userRepository.findById(transporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Transporter not found with ID: " + transporterId));

        if (transporter.getRole() != Role.TRANSPORTER) {
            throw new IllegalArgumentException("Selected user is not a transporter.");
        }

        Optional<Delivery> existing = deliveryRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            throw new IllegalStateException("A delivery is already assigned for order: " + order.getOrderNumber());
        }

        Delivery delivery = Delivery.builder()
                .order(order)
                .transporter(transporter)
                .assignedBy(assignedBy)
                .status(DeliveryStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .build();

        Delivery saved = deliveryRepository.save(delivery);

        DeliveryStatusUpdate initialUpdate = DeliveryStatusUpdate.builder()
                .delivery(saved)
                .status(DeliveryStatus.ASSIGNED)
                .notes("Delivery assigned to transporter: " + transporter.getFullName())
                .timestamp(LocalDateTime.now())
                .build();
        statusUpdateRepository.save(initialUpdate);

        notificationService.notifyUser(
                transporter,
                "New Delivery Assigned",
                "You have been assigned to deliver order: " + order.getOrderNumber(),
                "/transporter/deliveries"
        );

        notificationService.notifyUser(
                order.getBuyer(),
                "Order Dispatched",
                "Your order " + order.getOrderNumber() + " has been assigned to transporter " + transporter.getFullName(),
                "/orders/" + order.getId()
        );

        auditLogService.logAction(
                assignedBy,
                "ASSIGN_DELIVERY",
                "Delivery",
                saved.getId(),
                "Assigned order " + order.getOrderNumber() + " to transporter " + transporter.getFullName()
        );
        log.info("Order {} assigned to transporter {}", order.getOrderNumber(), transporter.getEmail());
        return saved;
    }

    @Transactional
    public void updateDeliveryStatus(Long deliveryId, User transporter, DeliveryUpdateRequest request) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found with ID: " + deliveryId));

        if (!delivery.getTransporter().getId().equals(transporter.getId())) {
            throw new IllegalArgumentException("Only the assigned transporter can update this delivery.");
        }

        delivery.setStatus(request.getStatus());
        if (request.getStatus() == DeliveryStatus.DELIVERED) {
            delivery.setDeliveredAt(LocalDateTime.now());
            delivery.getOrder().setStatus(OrderStatus.DELIVERED);
            orderRepository.save(delivery.getOrder());
        }
        deliveryRepository.save(delivery);

        DeliveryStatusUpdate update = DeliveryStatusUpdate.builder()
                .delivery(delivery)
                .status(request.getStatus())
                .notes(request.getNotes())
                .timestamp(LocalDateTime.now())
                .build();
        statusUpdateRepository.save(update);

        notificationService.notifyUser(
                delivery.getOrder().getBuyer(),
                "Delivery Status Update: " + request.getStatus().name(),
                "Your order " + delivery.getOrder().getOrderNumber() + " is now " + request.getStatus().name() +
                        (request.getNotes() != null ? " (" + request.getNotes() + ")" : ""),
                "/orders/" + delivery.getOrder().getId()
        );

        auditLogService.logAction(
                transporter,
                "UPDATE_DELIVERY_STATUS",
                "Delivery",
                delivery.getId(),
                "Updated delivery status to " + request.getStatus().name() + ". Notes: " + request.getNotes()
        );
        log.info("Delivery {} status updated to {} by transporter {}", deliveryId, request.getStatus(), transporter.getEmail());
    }

    public List<Delivery> getTransporterDeliveries(Long transporterId) {
        return deliveryRepository.findByTransporterId(transporterId);
    }

    public Optional<Delivery> getDeliveryByOrderId(Long orderId) {
        return deliveryRepository.findByOrderId(orderId);
    }

    public List<DeliveryStatusUpdate> getDeliveryHistory(Long deliveryId) {
        return statusUpdateRepository.findByDeliveryIdOrderByTimestampAsc(deliveryId);
    }

    public List<User> getAvailableTransporters() {
        return userRepository.findByRole(Role.TRANSPORTER);
    }
}
