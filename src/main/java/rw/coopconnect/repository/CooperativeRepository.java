package rw.coopconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rw.coopconnect.domain.Cooperative;

import java.util.List;
import java.util.Optional;

/**
 * FR-02: Cooperative persistence.
 */
@Repository
public interface CooperativeRepository extends JpaRepository<Cooperative, Long> {

    Optional<Cooperative> findByName(String name);

    List<Cooperative> findByDistrict(String district);

    boolean existsByName(String name);

    List<Cooperative> findByManagerId(Long managerId);
}
