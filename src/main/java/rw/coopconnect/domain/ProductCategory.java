package rw.coopconnect.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * FR-04: Product category (crop type) with English and Kinyarwanda names.
 * NFR-03: i18n support at the data level.
 */
@Entity
@Table(name = "product_categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "name_rw", length = 100)
    private String nameRw;

    @Column(length = 500)
    private String description;
}
