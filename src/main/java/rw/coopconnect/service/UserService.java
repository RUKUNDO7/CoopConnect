package rw.coopconnect.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.coopconnect.domain.Cart;
import rw.coopconnect.domain.Cooperative;
import rw.coopconnect.domain.FarmerProfile;
import rw.coopconnect.domain.User;
import rw.coopconnect.domain.enums.ApprovalStatus;
import rw.coopconnect.domain.enums.Role;
import rw.coopconnect.dto.request.RegistrationRequest;
import rw.coopconnect.exception.DuplicateResourceException;
import rw.coopconnect.exception.ResourceNotFoundException;
import rw.coopconnect.repository.CartRepository;
import rw.coopconnect.repository.CooperativeRepository;
import rw.coopconnect.repository.FarmerProfileRepository;
import rw.coopconnect.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * FR-01: User registration, profile retrieval, password management.
 * FR-02: Farmer profile creation with cooperative linkage and PENDING status.
 * NFR-01: BCrypt password hashing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FarmerProfileRepository farmerProfileRepository;
    private final CooperativeRepository cooperativeRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional
    public User registerUser(RegistrationRequest request) {
        // FR-01: Uniqueness checks
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new DuplicateResourceException("An account with this email already exists: " + request.getEmail());
        }
        if (userRepository.existsByPhone(request.getPhone().trim())) {
            throw new DuplicateResourceException("An account with this phone number already exists: " + request.getPhone());
        }

        // FR-02: National ID uniqueness check for farmers
        if (request.getRole() == Role.FARMER) {
            if (request.getNationalId() == null || request.getNationalId().isBlank()) {
                throw new IllegalArgumentException("National ID is required for farmer registration.");
            }
            if (farmerProfileRepository.existsByNationalId(request.getNationalId().trim())) {
                throw new DuplicateResourceException("A farmer with this National ID already exists.");
            }
            if (request.getCooperativeId() == null) {
                throw new IllegalArgumentException("Cooperative selection is required for farmer registration.");
            }
        }

        User user = User.builder()
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .phone(request.getPhone().trim())
                .role(request.getRole())
                .enabled(true)
                .build();

        user = userRepository.save(user);

        // FR-02: Create pending farmer profile if role is FARMER
        if (request.getRole() == Role.FARMER) {
            Cooperative cooperative = cooperativeRepository.findById(request.getCooperativeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cooperative", request.getCooperativeId()));

            FarmerProfile profile = FarmerProfile.builder()
                    .user(user)
                    .nationalId(request.getNationalId().trim())
                    .district(request.getDistrict() != null ? request.getDistrict().trim() : cooperative.getDistrict())
                    .sector(request.getSector() != null ? request.getSector().trim() : cooperative.getSector())
                    .cooperative(cooperative)
                    .approvalStatus(ApprovalStatus.PENDING)
                    .build();

            farmerProfileRepository.save(profile);
            log.info("Created pending farmer profile for user {} in cooperative {}", user.getEmail(), cooperative.getName());
        }

        // FR-06: Automatically initialize cart for BUYER
        if (request.getRole() == Role.BUYER) {
            Cart cart = Cart.builder()
                    .buyer(user)
                    .build();
            cartRepository.save(cart);
        }

        // FR-13: Audit log for registration
        auditLogService.log("USER_REGISTERED", "User", user.getId(),
                "Registered with role: " + user.getRole().name());

        return user;
    }

    @Transactional
    public String createPasswordResetToken(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with email: " + email));

        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetExpiry(LocalDateTime.now().plusHours(2));
        userRepository.save(user);

        auditLogService.log("PASSWORD_RESET_REQUESTED", "User", user.getId(), "Generated reset token");
        log.info("Password reset token generated for {}: {}", user.getEmail(), token);
        return token;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired password reset token."));

        if (user.getPasswordResetExpiry() == null || user.getPasswordResetExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Password reset token has expired.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiry(null);
        userRepository.save(user);

        auditLogService.log("PASSWORD_RESET_COMPLETED", "User", user.getId(), "Password reset successfully");
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
