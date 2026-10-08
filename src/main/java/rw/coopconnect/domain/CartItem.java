package rw.coopconnect.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * FR-06: Individual item in a shopping cart.
 */
@Entity
@Table(name = "cart_items", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"cart_id", "listing_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private ProductListing listing;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;
}
