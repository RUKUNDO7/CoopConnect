package rw.coopconnect.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rw.coopconnect.domain.ProductListing;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * FR-04, FR-05: Product listing persistence with search/filter and stock locking.
 * NFR-02: Paginated queries for performance with 10k+ products.
 * NFR-04: Pessimistic locking for stock reservation.
 */
@Repository
public interface ProductListingRepository extends JpaRepository<ProductListing, Long> {

    Page<ProductListing> findByActiveTrue(Pageable pageable);

    @Query("SELECT pl FROM ProductListing pl " +
           "JOIN pl.harvest h " +
           "JOIN h.category c " +
           "JOIN pl.cooperative co " +
           "WHERE pl.active = true " +
           "AND (:categoryId IS NULL OR c.id = :categoryId) " +
           "AND (:district IS NULL OR co.district = :district) " +
           "AND (:cooperativeId IS NULL OR co.id = :cooperativeId) " +
           "AND (:minPrice IS NULL OR pl.pricePerUnit >= :minPrice) " +
           "AND (:maxPrice IS NULL OR pl.pricePerUnit <= :maxPrice) " +
           "AND (:search IS NULL OR LOWER(pl.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ProductListing> searchListings(
            @Param("categoryId") Long categoryId,
            @Param("district") String district,
            @Param("cooperativeId") Long cooperativeId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("search") String search,
            Pageable pageable);

    // FR-06: Pessimistic lock for stock reservation (NFR-04)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pl FROM ProductListing pl WHERE pl.id = :id")
    Optional<ProductListing> findByIdWithLock(@Param("id") Long id);

    List<ProductListing> findByCooperativeId(Long cooperativeId);

    // FR-11: Price dashboard data
    @Query("SELECT c.name, co.district, AVG(pl.pricePerUnit) " +
           "FROM ProductListing pl " +
           "JOIN pl.harvest h " +
           "JOIN h.category c " +
           "JOIN pl.cooperative co " +
           "WHERE pl.active = true " +
           "GROUP BY c.name, co.district " +
           "ORDER BY c.name, co.district")
    List<Object[]> findAveragePriceByCropAndDistrict();
}
