package rw.coopconnect.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import rw.coopconnect.domain.Notification;
import rw.coopconnect.security.CustomUserDetails;
import rw.coopconnect.service.NotificationService;

/**
 * FR-09: Notifications center and status management.
 */
@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/notifications")
    public String viewNotifications(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        Page<Notification> notifications = notificationService.getUserNotifications(
                userDetails.getId(), PageRequest.of(page, size)
        );
        model.addAttribute("notifications", notifications.getContent());
        model.addAttribute("page", notifications);
        return "notifications/list";
    }

    @PostMapping("/notifications/{id}/read")
    public String markAsRead(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "redirect", required = false) String redirect) {
        notificationService.markAsRead(id, userDetails.getId());
        if (redirect != null && !redirect.isEmpty()) {
            return "redirect:" + redirect;
        }
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String markAllAsRead(@AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAllAsRead(userDetails.getId());
        return "redirect:/notifications";
    }
}
