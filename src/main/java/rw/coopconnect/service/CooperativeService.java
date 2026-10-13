package rw.coopconnect.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.coopconnect.domain.Cooperative;
import rw.coopconnect.domain.FarmerProfile;
import rw.coopconnect.domain.User;
import rw.coopconnect.domain.enums.ApprovalStatus;
import rw.coopconnect.exception.ResourceNotFoundException;
import rw.coopconnect.repository.CooperativeRepository;
import rw.coopconnect.repository.FarmerProfileRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FR-02: Cooperative and farmer membership management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CooperativeService {

    private final CooperativeRepository cooperativeRepository;
    private final FarmerProfileRepository farmerProfileRepository;
    private final AuditLogService auditLogService;

    public List<Cooperative> getAllCooperatives() {
        return cooperativeRepository.findAll();
    }

    public Cooperative getCooperativeById(Long id) {
        return cooperativeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cooperative not found with ID: " + id));
    }

    public Cooperative getCooperativeByManager(Long managerId) {
        return cooperativeRepository.findByManagerId(managerId).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No cooperative assigned to manager ID: " + managerId));
    }

    public List<FarmerProfile> getFarmersByCooperative(Long cooperativeId) {
        return farmerProfileRepository.findByCooperativeId(cooperativeId);
    }

    public List<FarmerProfile> getPendingFarmersByCooperative(Long cooperativeId) {
        return farmerProfileRepository.findByCooperativeIdAndApprovalStatus(cooperativeId, ApprovalStatus.PENDING);
    }

    @Transactional
    public void approveFarmer(Long profileId, User manager) {
        FarmerProfile profile = farmerProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found with ID: " + profileId));

        profile.setApprovalStatus(ApprovalStatus.APPROVED);
        profile.setApprovedAt(LocalDateTime.now());
        profile.setApprovedBy(manager);
        farmerProfileRepository.save(profile);

        auditLogService.logAction(
                manager,
                "APPROVE_FARMER",
                "FarmerProfile",
                profile.getId(),
                "Approved farmer: " + profile.getUser().getFullName() + " (National ID: " + profile.getNationalId() + ")"
        );
        log.info("Manager {} approved farmer {}", manager.getEmail(), profile.getUser().getEmail());
    }

    @Transactional
    public void rejectFarmer(Long profileId, User manager, String reason) {
        FarmerProfile profile = farmerProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found with ID: " + profileId));

        profile.setApprovalStatus(ApprovalStatus.REJECTED);
        farmerProfileRepository.save(profile);

        auditLogService.logAction(
                manager,
                "REJECT_FARMER",
                "FarmerProfile",
                profile.getId(),
                "Rejected farmer: " + profile.getUser().getFullName() + ". Reason: " + reason
        );
        log.info("Manager {} rejected farmer {}: {}", manager.getEmail(), profile.getUser().getEmail(), reason);
    }
}
