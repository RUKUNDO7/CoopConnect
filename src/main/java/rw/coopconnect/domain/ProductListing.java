package rw.coopconnect.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * FR-04: Product listing created from a verified harvest.
 * NFR-02: Indexed on active, cooperative_id, category for fast filtering.
 */
@Entity
@Table(name = "product_listings", indexes = {
    @Index(name = "idx_listing_active", columnList = "active"),
    @Index(name = "idx_listing_cooperative", columnList = "cooperative_id"),
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "harvest_id", nullable = false, unique = true)
    private Harvest harvest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cooperative_id", nullable = false)
    private Cooperative cooperative;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "price_per_unit", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerUnit;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "RWF";

    @Column(name = "available_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal availableQuantity;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
