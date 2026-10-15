package rw.coopconnect.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import rw.coopconnect.domain.enums.Role;
import rw.coopconnect.dto.request.NewPasswordRequest;
import rw.coopconnect.dto.request.PasswordResetRequest;
import rw.coopconnect.dto.request.RegistrationRequest;
import rw.coopconnect.repository.CooperativeRepository;
import rw.coopconnect.service.UserService;

/**
 * FR-01: Authentication controller for login, registration, and password reset.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final CooperativeRepository cooperativeRepository;

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email or password.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully.");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("registrationRequest")) {
            model.addAttribute("registrationRequest", new RegistrationRequest());
        }
        model.addAttribute("roles", Role.values());
        model.addAttribute("cooperatives", cooperativeRepository.findAll());
        return "auth/register";
    }

    @PostMapping("/register")
    public String handleRegister(
            @Valid @ModelAttribute("registrationRequest") RegistrationRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", Role.values());
            model.addAttribute("cooperatives", cooperativeRepository.findAll());
            return "auth/register";
        }

        try {
            userService.registerUser(request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Registration successful! You may now sign in.");
            return "redirect:/login";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("roles", Role.values());
            model.addAttribute("cooperatives", cooperativeRepository.findAll());
            return "auth/register";
        }
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(Model model) {
        model.addAttribute("passwordResetRequest", new PasswordResetRequest());
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String handlePasswordResetRequest(
            @Valid @ModelAttribute("passwordResetRequest") PasswordResetRequest request,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "auth/reset-password";
        }

        try {
            String token = userService.createPasswordResetToken(request.getEmail());
            // In a production setup, an email with the link is sent. For demo/dev:
            model.addAttribute("successMessage",
                    "Password reset instructions have been generated. Click the link below to set your new password:");
            model.addAttribute("resetLink", "/reset-password/confirm?token=" + token);
            return "auth/reset-password";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/reset-password";
        }
    }

    @GetMapping("/reset-password/confirm")
    public String confirmResetPasswordPage(@RequestParam("token") String token, Model model) {
        NewPasswordRequest req = new NewPasswordRequest();
        req.setToken(token);
        model.addAttribute("newPasswordRequest", req);
        return "auth/new-password";
    }

    @PostMapping("/reset-password/confirm")
    public String handleConfirmResetPassword(
            @Valid @ModelAttribute("newPasswordRequest") NewPasswordRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "auth/new-password";
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            model.addAttribute("errorMessage", "Passwords do not match.");
            return "auth/new-password";
        }

        try {
            userService.resetPassword(request.getToken(), request.getNewPassword());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Password successfully updated. Please login with your new password.");
            return "redirect:/login";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/new-password";
        }
    }
}
