package rw.coopconnect.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import rw.coopconnect.domain.FarmerProfile;
import rw.coopconnect.domain.Harvest;
import rw.coopconnect.domain.enums.ApprovalStatus;
import rw.coopconnect.domain.enums.HarvestStatus;
import rw.coopconnect.dto.request.HarvestDeclarationRequest;
import rw.coopconnect.repository.FarmerProfileRepository;
import rw.coopconnect.repository.ProductCategoryRepository;
import rw.coopconnect.security.CustomUserDetails;
import rw.coopconnect.service.HarvestService;

import java.util.List;

/**
 * FR-02, FR-03: Farmer controller for dashboard, cooperative status, and harvest declarations.
 */
@Controller
@RequestMapping("/farmer")
@RequiredArgsConstructor
public class FarmerController {

    private final HarvestService harvestService;
    private final FarmerProfileRepository farmerProfileRepository;
    private final ProductCategoryRepository categoryRepository;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        FarmerProfile profile = farmerProfileRepository.findByUserId(userDetails.getUser().getId())
                .orElse(null);

        List<Harvest> harvests = harvestService.getHarvestsByFarmer(userDetails.getUser().getId());

        long pendingCount = harvests.stream().filter(h -> h.getStatus() == HarvestStatus.PENDING).count();
        long verifiedCount = harvests.stream().filter(h -> h.getStatus() == HarvestStatus.VERIFIED).count();
        long rejectedCount = harvests.stream().filter(h -> h.getStatus() == HarvestStatus.REJECTED).count();

        model.addAttribute("profile", profile);
        model.addAttribute("totalHarvests", harvests.size());
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("verifiedCount", verifiedCount);
        model.addAttribute("rejectedCount", rejectedCount);
        model.addAttribute("recentHarvests", harvests.stream().limit(5).toList());

        return "farmer/dashboard";
    }

    @GetMapping("/harvests")
    public String listHarvests(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<Harvest> harvests = harvestService.getHarvestsByFarmer(userDetails.getUser().getId());
        model.addAttribute("harvests", harvests);
        return "farmer/harvests";
    }

    @GetMapping("/harvests/new")
    public String newHarvestForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        FarmerProfile profile = farmerProfileRepository.findByUserId(userDetails.getUser().getId()).orElse(null);
        if (profile != null && profile.getApprovalStatus() != ApprovalStatus.APPROVED) {
            model.addAttribute("errorMessage", "Your farmer profile is currently " + profile.getApprovalStatus() +
                    ". You can only declare harvests once approved by your cooperative manager.");
            return "farmer/dashboard";
        }

        if (!model.containsAttribute("harvestRequest")) {
            model.addAttribute("harvestRequest", new HarvestDeclarationRequest());
        }
        model.addAttribute("categories", categoryRepository.findAll());
        return "farmer/declare-harvest";
    }

    @PostMapping("/harvests")
    public String submitHarvest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute("harvestRequest") HarvestDeclarationRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryRepository.findAll());
            return "farmer/declare-harvest";
        }

        try {
            harvestService.declareHarvest(userDetails.getUser(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Harvest declared successfully! Pending manager verification.");
            return "redirect:/farmer/harvests";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("categories", categoryRepository.findAll());
            return "farmer/declare-harvest";
        }
    }
}
