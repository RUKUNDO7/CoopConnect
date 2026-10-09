package rw.coopconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rw.coopconnect.domain.ProductCategory;

import java.util.Optional;

/**
 * FR-04: Product category persistence.
 */
@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    Optional<ProductCategory> findByName(String name);

    boolean existsByName(String name);
}
