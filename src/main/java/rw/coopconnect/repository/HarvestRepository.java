package rw.coopconnect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rw.coopconnect.domain.Harvest;
import rw.coopconnect.domain.enums.HarvestStatus;

import java.util.List;

/**
 * FR-03: Harvest persistence with farmer and status queries.
 */
@Repository
public interface HarvestRepository extends JpaRepository<Harvest, Long> {

    List<Harvest> findByFarmerId(Long farmerId);

    List<Harvest> findByFarmerIdAndStatus(Long farmerId, HarvestStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT h FROM Harvest h, FarmerProfile fp " +
           "WHERE h.farmer.id = fp.user.id AND h.status = :status AND fp.cooperative.id = :cooperativeId")
    List<Harvest> findByStatusAndCooperativeId(
            @org.springframework.data.repository.query.Param("status") HarvestStatus status,
            @org.springframework.data.repository.query.Param("cooperativeId") Long cooperativeId);
}
