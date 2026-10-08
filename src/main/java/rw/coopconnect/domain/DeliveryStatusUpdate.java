package rw.coopconnect.domain;

import jakarta.persistence.*;
import lombok.*;
import rw.coopconnect.domain.enums.DeliveryStatus;

import java.time.LocalDateTime;

/**
 * FR-08: Timestamped delivery status change record.
 */
@Entity
@Table(name = "delivery_status_updates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeliveryStatusUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus status;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}
