package rw.coopconnect.controller.advice;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import rw.coopconnect.domain.Cart;
import rw.coopconnect.domain.Notification;
import rw.coopconnect.domain.enums.Role;
import rw.coopconnect.security.CustomUserDetails;
import rw.coopconnect.service.CartService;
import rw.coopconnect.service.NotificationService;

import java.util.Collections;
import java.util.List;

/**
 * Provides global navigation and contextual model attributes across all Thymeleaf templates.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final NotificationService notificationService;
    private final CartService cartService;

    @ModelAttribute("currentUser")
    public CustomUserDetails currentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return userDetails;
    }

    @ModelAttribute("unreadNotificationCount")
    public long unreadNotificationCount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null) {
            return notificationService.getUnreadCount(userDetails.getId());
        }
        return 0;
    }

    @ModelAttribute("recentNotifications")
    public List<Notification> recentNotifications(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null) {
            return notificationService.getUnreadNotifications(userDetails.getId());
        }
        return Collections.emptyList();
    }

    @ModelAttribute("cartItemCount")
    public int cartItemCount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.getRole() == Role.BUYER) {
            try {
                Cart cart = cartService.getOrCreateCart(userDetails.getUser());
                return cart.getItems().size();
            } catch (Exception ignored) {
            }
        }
        return 0;
    }
}
