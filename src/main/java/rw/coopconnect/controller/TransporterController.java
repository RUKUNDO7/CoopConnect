package rw.coopconnect.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import rw.coopconnect.domain.Delivery;
import rw.coopconnect.domain.enums.DeliveryStatus;
import rw.coopconnect.dto.request.DeliveryUpdateRequest;
import rw.coopconnect.security.CustomUserDetails;
import rw.coopconnect.service.DeliveryService;

import java.util.List;

/**
 * FR-08: Transporter controller for delivery management and status tracking.
 */
@Slf4j
@Controller
@RequestMapping("/transporter")
@RequiredArgsConstructor
public class TransporterController {

    private final DeliveryService deliveryService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<Delivery> deliveries = deliveryService.getTransporterDeliveries(userDetails.getId());

        long assignedCount = deliveries.stream().filter(d -> d.getStatus() == DeliveryStatus.ASSIGNED).count();
        long inTransitCount = deliveries.stream().filter(d -> d.getStatus() == DeliveryStatus.IN_TRANSIT || d.getStatus() == DeliveryStatus.PICKED_UP).count();
        long deliveredCount = deliveries.stream().filter(d -> d.getStatus() == DeliveryStatus.DELIVERED).count();

        model.addAttribute("deliveries", deliveries);
        model.addAttribute("assignedCount", assignedCount);
        model.addAttribute("inTransitCount", inTransitCount);
        model.addAttribute("deliveredCount", deliveredCount);
        return "transporter/dashboard";
    }

    @GetMapping("/deliveries")
    public String listDeliveries(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<Delivery> deliveries = deliveryService.getTransporterDeliveries(userDetails.getId());
        model.addAttribute("deliveries", deliveries);
        model.addAttribute("availableStatuses", DeliveryStatus.values());
        return "transporter/deliveries";
    }

    @PostMapping("/deliveries/{id}/update")
    public String updateDelivery(
            @PathVariable("id") Long deliveryId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute DeliveryUpdateRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid delivery status.");
            return "redirect:/transporter/deliveries";
        }

        try {
            deliveryService.updateDeliveryStatus(deliveryId, userDetails.getUser(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Delivery status updated to " + request.getStatus().name());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/transporter/deliveries";
    }
}
