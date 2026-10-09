package rw.coopconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rw.coopconnect.domain.FarmerProfile;
import rw.coopconnect.domain.enums.ApprovalStatus;

import java.util.List;
import java.util.Optional;

/**
 * FR-02: Farmer profile persistence with approval queries.
 */
@Repository
public interface FarmerProfileRepository extends JpaRepository<FarmerProfile, Long> {

    Optional<FarmerProfile> findByUserId(Long userId);

    boolean existsByNationalId(String nationalId);

    boolean existsByUserId(Long userId);

    List<FarmerProfile> findByCooperativeIdAndApprovalStatus(Long cooperativeId, ApprovalStatus status);

    List<FarmerProfile> findByCooperativeId(Long cooperativeId);
}
