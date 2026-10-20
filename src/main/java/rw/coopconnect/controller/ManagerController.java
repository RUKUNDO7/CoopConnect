package rw.coopconnect.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import rw.coopconnect.domain.*;
import rw.coopconnect.dto.request.ProductListingRequest;
import rw.coopconnect.security.CustomUserDetails;
import rw.coopconnect.service.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * FR-02, FR-03, FR-04, FR-08, FR-12: Cooperative Manager dashboard, farmer approvals,
 * harvest verification, listing publication, delivery dispatch, and CSV sales reporting.
 */
@Slf4j
@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerController {

    private final CooperativeService cooperativeService;
    private final HarvestService harvestService;
    private final ProductListingService listingService;
    private final OrderService orderService;
    private final DeliveryService deliveryService;
    private final ReportService reportService;
    private final FileStorageService fileStorageService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Cooperative coop = cooperativeService.getCooperativeByManager(userDetails.getId());

        List<FarmerProfile> pendingFarmers = cooperativeService.getPendingFarmersByCooperative(coop.getId());
        List<Harvest> pendingHarvests = harvestService.getPendingHarvestsByCooperative(coop.getId());
        List<ProductListing> listings = listingService.getListingsByCooperative(coop.getId());
        Page<Order> recentOrders = orderService.getOrdersByCooperative(coop.getId(), PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")));

        model.addAttribute("cooperative", coop);
        model.addAttribute("pendingFarmersCount", pendingFarmers.size());
        model.addAttribute("pendingHarvestsCount", pendingHarvests.size());
        model.addAttribute("totalListingsCount", listings.size());
        model.addAttribute("recentOrders", recentOrders.getContent());

        return "manager/dashboard";
    }

    @GetMapping("/farmers")
    public String listFarmers(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Cooperative coop = cooperativeService.getCooperativeByManager(userDetails.getId());
        model.addAttribute("cooperative", coop);
        model.addAttribute("farmers", cooperativeService.getFarmersByCooperative(coop.getId()));
        return "manager/farmers";
    }

    @PostMapping("/farmers/{id}/approve")
    public String approveFarmer(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            cooperativeService.approveFarmer(id, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Farmer profile approved successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/manager/farmers";
    }

    @PostMapping("/farmers/{id}/reject")
    public String rejectFarmer(
            @PathVariable("id") Long id,
            @RequestParam("reason") String reason,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            cooperativeService.rejectFarmer(id, userDetails.getUser(), reason);
            redirectAttributes.addFlashAttribute("successMessage", "Farmer profile rejected.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/manager/farmers";
    }

    @GetMapping("/harvests")
    public String listHarvests(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Cooperative coop = cooperativeService.getCooperativeByManager(userDetails.getId());
        model.addAttribute("cooperative", coop);
        model.addAttribute("pendingHarvests", harvestService.getPendingHarvestsByCooperative(coop.getId()));
        model.addAttribute("verifiedHarvests", harvestService.getVerifiedHarvestsByCooperative(coop.getId()));
        return "manager/harvests";
    }

    @PostMapping("/harvests/{id}/verify")
    public String verifyHarvest(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            harvestService.verifyHarvest(id, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Harvest verified! It can now be published as a marketplace listing.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/manager/harvests";
    }

    @PostMapping("/harvests/{id}/reject")
    public String rejectHarvest(
            @PathVariable("id") Long id,
            @RequestParam("reason") String reason,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            harvestService.rejectHarvest(id, userDetails.getUser(), reason);
            redirectAttributes.addFlashAttribute("successMessage", "Harvest rejected with reason provided.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/manager/harvests";
    }

    @GetMapping("/listings")
    public String listListings(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Cooperative coop = cooperativeService.getCooperativeByManager(userDetails.getId());
        model.addAttribute("cooperative", coop);
        model.addAttribute("listings", listingService.getListingsByCooperative(coop.getId()));
        return "manager/listings";
    }

    @GetMapping("/listings/new")
    public String newListingForm(
            @RequestParam(value = "harvestId", required = false) Long harvestId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        Cooperative coop = cooperativeService.getCooperativeByManager(userDetails.getId());
        ProductListingRequest request = new ProductListingRequest();

        if (harvestId != null) {
            Harvest harvest = harvestService.getHarvestById(harvestId);
            request.setHarvestId(harvest.getId());
            request.setTitle(harvest.getCategory().getName() + " (" + harvest.getQualityGrade().name() + ")");
            request.setAvailableQuantity(harvest.getQuantity());
            request.setUnit(harvest.getUnit());
            model.addAttribute("selectedHarvest", harvest);
        }

        model.addAttribute("listingRequest", request);
        model.addAttribute("verifiedHarvests", harvestService.getVerifiedHarvestsByCooperative(coop.getId()));
        return "manager/create-listing";
    }

    @PostMapping("/listings")
    public String createListing(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute("listingRequest") ProductListingRequest request,
            BindingResult bindingResult,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            Model model,
            RedirectAttributes redirectAttributes) {

        Cooperative coop = cooperativeService.getCooperativeByManager(userDetails.getId());

        if (bindingResult.hasErrors()) {
            model.addAttribute("verifiedHarvests", harvestService.getVerifiedHarvestsByCooperative(coop.getId()));
            return "manager/create-listing";
        }

        try {
            if (photo != null && !photo.isEmpty()) {
                String photoUrl = fileStorageService.storeFile(photo);
                request.setPhotoUrl(photoUrl);
            }

            listingService.createListing(coop, userDetails.getUser(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Listing published successfully to the marketplace!");
            return "redirect:/manager/listings";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("verifiedHarvests", harvestService.getVerifiedHarvestsByCooperative(coop.getId()));
            return "manager/create-listing";
        }
    }

    @PostMapping("/listings/{id}/toggle")
    public String toggleListing(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            listingService.toggleListingStatus(id, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Listing status updated.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/manager/listings";
    }

    @GetMapping("/orders")
    public String listOrders(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        Cooperative coop = cooperativeService.getCooperativeByManager(userDetails.getId());
        Page<Order> orderPage = orderService.getOrdersByCooperative(
                coop.getId(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("page", orderPage);
        model.addAttribute("transporters", deliveryService.getAvailableTransporters());
        return "manager/orders";
    }

    @PostMapping("/orders/{id}/assign-delivery")
    public String assignDelivery(
            @PathVariable("id") Long orderId,
            @RequestParam("transporterId") Long transporterId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            deliveryService.assignTransporter(orderId, transporterId, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Transporter successfully assigned for delivery!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/manager/orders";
    }

    @GetMapping("/reports")
    public String reportsPage(Model model) {
        model.addAttribute("startDate", LocalDate.now().minusMonths(1));
        model.addAttribute("endDate", LocalDate.now());
        return "manager/reports";
    }

    @GetMapping("/reports/export")
    public ResponseEntity<byte[]> exportReports(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        byte[] csvData = reportService.exportOrdersToCsv(start, end);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"coopconnect-orders-" + startDate + "-to-" + endDate + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }
}
