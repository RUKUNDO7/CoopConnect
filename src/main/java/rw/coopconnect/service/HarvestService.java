package rw.coopconnect.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.coopconnect.domain.Harvest;
import rw.coopconnect.domain.ProductCategory;
import rw.coopconnect.domain.User;
import rw.coopconnect.domain.enums.HarvestStatus;
import rw.coopconnect.dto.request.HarvestDeclarationRequest;
import rw.coopconnect.exception.ResourceNotFoundException;
import rw.coopconnect.repository.HarvestRepository;
import rw.coopconnect.repository.ProductCategoryRepository;

import java.util.List;

/**
 * FR-03: Harvest declaration and cooperative manager verification.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HarvestService {

    private final HarvestRepository harvestRepository;
    private final ProductCategoryRepository categoryRepository;
    private final AuditLogService auditLogService;

    public List<Harvest> getHarvestsByFarmer(Long farmerId) {
        return harvestRepository.findByFarmerId(farmerId);
    }

    public List<Harvest> getPendingHarvestsByCooperative(Long cooperativeId) {
        return harvestRepository.findByStatusAndCooperativeId(HarvestStatus.PENDING, cooperativeId);
    }

    public List<Harvest> getVerifiedHarvestsByCooperative(Long cooperativeId) {
        return harvestRepository.findByStatusAndCooperativeId(HarvestStatus.VERIFIED, cooperativeId);
    }

    public Harvest getHarvestById(Long id) {
        return harvestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Harvest not found with ID: " + id));
    }

    @Transactional
    public Harvest declareHarvest(User farmer, HarvestDeclarationRequest request) {
        ProductCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Product category not found with ID: " + request.getCategoryId()));

        Harvest harvest = Harvest.builder()
                .farmer(farmer)
                .category(category)
                .quantity(request.getQuantity())
                .unit(request.getUnit())
                .expectedDate(request.getExpectedDate())
                .qualityGrade(request.getQualityGrade())
                .status(HarvestStatus.PENDING)
                .build();

        Harvest saved = harvestRepository.save(harvest);

        auditLogService.logAction(
                farmer,
                "DECLARE_HARVEST",
                "Harvest",
                saved.getId(),
                "Declared harvest of " + saved.getQuantity() + " " + saved.getUnit() + " of " + category.getName()
        );
        log.info("Farmer {} declared harvest ID {}", farmer.getEmail(), saved.getId());
        return saved;
    }

    @Transactional
    public void verifyHarvest(Long harvestId, User manager) {
        Harvest harvest = getHarvestById(harvestId);
        harvest.setStatus(HarvestStatus.VERIFIED);
        harvest.setVerifiedBy(manager);
        harvestRepository.save(harvest);

        auditLogService.logAction(
                manager,
                "VERIFY_HARVEST",
                "Harvest",
                harvest.getId(),
                "Verified harvest of " + harvest.getQuantity() + " " + harvest.getUnit() + " of " + harvest.getCategory().getName()
        );
        log.info("Manager {} verified harvest ID {}", manager.getEmail(), harvest.getId());
    }

    @Transactional
    public void rejectHarvest(Long harvestId, User manager, String rejectionReason) {
        Harvest harvest = getHarvestById(harvestId);
        harvest.setStatus(HarvestStatus.REJECTED);
        harvest.setRejectionReason(rejectionReason);
        harvest.setVerifiedBy(manager);
        harvestRepository.save(harvest);

        auditLogService.logAction(
                manager,
                "REJECT_HARVEST",
                "Harvest",
                harvest.getId(),
                "Rejected harvest ID " + harvest.getId() + ". Reason: " + rejectionReason
        );
        log.info("Manager {} rejected harvest ID {}: {}", manager.getEmail(), harvest.getId(), rejectionReason);
    }
}
