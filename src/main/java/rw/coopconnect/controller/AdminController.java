package rw.coopconnect.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import rw.coopconnect.domain.AuditLog;
import rw.coopconnect.domain.Cooperative;
import rw.coopconnect.domain.Order;
import rw.coopconnect.domain.User;
import rw.coopconnect.domain.enums.OrderStatus;
import rw.coopconnect.domain.enums.Role;
import rw.coopconnect.repository.CooperativeRepository;
import rw.coopconnect.repository.OrderRepository;
import rw.coopconnect.repository.ProductListingRepository;
import rw.coopconnect.repository.UserRepository;
import rw.coopconnect.security.CustomUserDetails;
import rw.coopconnect.service.AuditLogService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * FR-13: System Administrator dashboard, user management, cooperatives management,
 * and security audit logs.
 */
@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final CooperativeRepository cooperativeRepository;
    private final ProductListingRepository listingRepository;
    private final OrderRepository orderRepository;
    private final AuditLogService auditLogService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long totalUsers = userRepository.count();
        long totalCoops = cooperativeRepository.count();
        long totalListings = listingRepository.count();
        long totalOrders = orderRepository.count();

        List<Order> orders = orderRepository.findAll();
        BigDecimal totalGmv = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID || o.getStatus() == OrderStatus.DELIVERED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalCoops", totalCoops);
        model.addAttribute("totalListings", totalListings);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalGmv", totalGmv);
        model.addAttribute("recentOrders", orders.stream().sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())).limit(5).toList());

        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUserStatus(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            user.setEnabled(!Boolean.TRUE.equals(user.getEnabled()));
            userRepository.save(user);

            auditLogService.logAction(
                    userDetails.getUser(),
                    "TOGGLE_USER_STATUS",
                    "User",
                    user.getId(),
                    "Changed user " + user.getEmail() + " enabled status to: " + user.getEnabled()
            );
            redirectAttributes.addFlashAttribute("successMessage", "User status updated.");
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/cooperatives")
    public String listCooperatives(Model model) {
        model.addAttribute("cooperatives", cooperativeRepository.findAll());
        model.addAttribute("managers", userRepository.findByRole(Role.COOPERATIVE_MANAGER));
        return "admin/cooperatives";
    }

    @PostMapping("/cooperatives")
    public String createCooperative(
            @RequestParam("name") String name,
            @RequestParam("district") String district,
            @RequestParam("sector") String sector,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "managerId", required = false) Long managerId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            User manager = (managerId != null) ? userRepository.findById(managerId).orElse(null) : null;

            Cooperative coop = Cooperative.builder()
                    .name(name)
                    .district(district)
                    .sector(sector)
                    .description(description)
                    .manager(manager)
                    .createdAt(LocalDateTime.now())
                    .build();

            Cooperative saved = cooperativeRepository.save(coop);

            auditLogService.logAction(
                    userDetails.getUser(),
                    "CREATE_COOPERATIVE",
                    "Cooperative",
                    saved.getId(),
                    "Created cooperative: " + saved.getName() + " in " + saved.getDistrict()
            );
            redirectAttributes.addFlashAttribute("successMessage", "Cooperative created successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/cooperatives";
    }

    @GetMapping("/audit-logs")
    public String auditLogs(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Model model) {
        Page<AuditLog> logPage = auditLogService.getLogs(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp")));
        model.addAttribute("logs", logPage.getContent());
        model.addAttribute("page", logPage);
        return "admin/audit-logs";
    }
}
