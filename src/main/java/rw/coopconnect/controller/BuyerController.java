package rw.coopconnect.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import rw.coopconnect.domain.*;
import rw.coopconnect.domain.enums.OrderStatus;
import rw.coopconnect.dto.request.AddToCartRequest;
import rw.coopconnect.dto.request.CheckoutRequest;
import rw.coopconnect.dto.request.RatingRequest;
import rw.coopconnect.repository.PaymentRepository;
import rw.coopconnect.security.CustomUserDetails;
import rw.coopconnect.service.*;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * FR-06, FR-07, FR-10: Buyer shopping cart, checkout, payments, order tracking, and ratings.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class BuyerController {

    private final CartService cartService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final DeliveryService deliveryService;
    private final RatingService ratingService;
    private final PaymentRepository paymentRepository;

    @GetMapping("/buyer/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Page<Order> orders = orderService.getOrdersByBuyer(
                userDetails.getId(), PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        Cart cart = cartService.getOrCreateCart(userDetails.getUser());

        model.addAttribute("orders", orders.getContent());
        model.addAttribute("totalOrders", orders.getTotalElements());
        model.addAttribute("cartItemCount", cart.getItems().size());
        return "buyer/dashboard";
    }

    @GetMapping("/cart")
    public String viewCart(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Cart cart = cartService.getOrCreateCart(userDetails.getUser());
        BigDecimal total = cartService.calculateCartTotal(cart);

        model.addAttribute("cart", cart);
        model.addAttribute("cartTotal", total);
        return "buyer/cart";
    }

    @PostMapping("/cart/add")
    public String addToCart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute AddToCartRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid quantity requested.");
            return "redirect:/products/" + request.getListingId();
        }

        try {
            cartService.addItemToCart(userDetails.getUser(), request.getListingId(), request.getQuantity());
            redirectAttributes.addFlashAttribute("successMessage", "Item added to cart successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/update")
    public String updateCart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("cartItemId") Long cartItemId,
            @RequestParam("quantity") BigDecimal quantity,
            RedirectAttributes redirectAttributes) {
        try {
            cartService.updateItemQuantity(userDetails.getUser(), cartItemId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Cart updated.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove/{id}")
    public String removeFromCart(
            @PathVariable("id") Long cartItemId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            cartService.removeItemFromCart(userDetails.getUser(), cartItemId);
            redirectAttributes.addFlashAttribute("successMessage", "Item removed from cart.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/cart";
    }

    @GetMapping("/checkout")
    public String checkoutPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Cart cart = cartService.getOrCreateCart(userDetails.getUser());
        if (cart.getItems().isEmpty()) {
            return "redirect:/cart";
        }

        CheckoutRequest request = new CheckoutRequest();
        request.setPhoneNumber(userDetails.getPhone());

        model.addAttribute("cart", cart);
        model.addAttribute("cartTotal", cartService.calculateCartTotal(cart));
        model.addAttribute("checkoutRequest", request);
        return "buyer/checkout";
    }

    @PostMapping("/checkout")
    public String processCheckout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute("checkoutRequest") CheckoutRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        Cart cart = cartService.getOrCreateCart(userDetails.getUser());
        if (bindingResult.hasErrors()) {
            model.addAttribute("cart", cart);
            model.addAttribute("cartTotal", cartService.calculateCartTotal(cart));
            return "buyer/checkout";
        }

        try {
            // FR-06: Create order with pessimistic stock reservation
            Order order = orderService.createOrderFromCart(userDetails.getUser());

            // FR-07: Initiate mobile money payment
            Payment payment = paymentService.initiatePayment(order.getId(), userDetails.getUser(), request);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Order created successfully! Please confirm payment via your mobile wallet.");
            return "redirect:/orders/" + order.getId();
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("cart", cart);
            model.addAttribute("cartTotal", cartService.calculateCartTotal(cart));
            return "buyer/checkout";
        }
    }

    @GetMapping("/orders")
    public String listOrders(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        Page<Order> orderPage = orderService.getOrdersByBuyer(
                userDetails.getId(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("page", orderPage);
        return "buyer/order-history";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        Order order = orderService.getOrderById(id);

        // Security check: ensure buyer or authorized user
        if (!order.getBuyer().getId().equals(userDetails.getId()) &&
                !userDetails.getRole().name().equals("ADMIN") &&
                !userDetails.getRole().name().equals("COOPERATIVE_MANAGER")) {
            throw new IllegalArgumentException("Unauthorized to view this order.");
        }

        Optional<Payment> payment = paymentRepository.findByOrderId(order.getId()).stream().findFirst();
        Optional<Delivery> delivery = deliveryService.getDeliveryByOrderId(order.getId());
        Optional<Rating> rating = ratingService.getRatingByOrderId(order.getId());

        model.addAttribute("order", order);
        model.addAttribute("payment", payment.orElse(null));
        model.addAttribute("delivery", delivery.orElse(null));
        model.addAttribute("rating", rating.orElse(null));

        if (delivery.isPresent()) {
            model.addAttribute("deliveryUpdates", deliveryService.getDeliveryHistory(delivery.get().getId()));
        }

        if (order.getStatus() == OrderStatus.DELIVERED && rating.isEmpty()) {
            RatingRequest ratingRequest = new RatingRequest();
            ratingRequest.setOrderId(order.getId());
            ratingRequest.setScore(5);
            model.addAttribute("ratingRequest", ratingRequest);
        }

        return "buyer/order-detail";
    }

    @PostMapping("/orders/{id}/pay")
    public String simulatePayment(
            @PathVariable("id") Long orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            Payment payment = paymentRepository.findByOrderId(orderId).stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("No payment record found for this order."));

            paymentService.completePaymentImmediately(payment.getId(), userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage", "Payment confirmed successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/orders/" + orderId;
    }

    @PostMapping("/ratings")
    public String submitRating(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute("ratingRequest") RatingRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Rating submission invalid. Please select 1-5 stars.");
            return "redirect:/orders/" + request.getOrderId();
        }

        try {
            ratingService.submitRating(userDetails.getUser(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Thank you for rating your order!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/orders/" + request.getOrderId();
    }
}
