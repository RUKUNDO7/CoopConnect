package rw.coopconnect.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import rw.coopconnect.domain.enums.Role;
import rw.coopconnect.security.CustomUserDetails;

/**
 * FR-01: Routes authenticated users to their corresponding role dashboard.
 */
@Controller
@RequiredArgsConstructor
public class DashboardController {

    @GetMapping("/")
    public String index(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() &&
                authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return redirectToRoleDashboard(userDetails.getRole());
        }
        return "redirect:/products";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return redirectToRoleDashboard(userDetails.getRole());
        }
        return "redirect:/login";
    }

    private String redirectToRoleDashboard(Role role) {
        return switch (role) {
            case FARMER -> "redirect:/farmer/dashboard";
            case COOPERATIVE_MANAGER -> "redirect:/manager/dashboard";
            case BUYER -> "redirect:/buyer/dashboard";
            case TRANSPORTER -> "redirect:/transporter/dashboard";
            case ADMIN -> "redirect:/admin/dashboard";
        };
    }
}
