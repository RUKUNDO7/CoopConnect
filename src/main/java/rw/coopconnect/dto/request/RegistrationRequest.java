package rw.coopconnect.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rw.coopconnect.domain.enums.Role;

/**
 * FR-01, FR-02: User registration DTO.
 * Supports standard roles (BUYER, TRANSPORTER, COOPERATIVE_MANAGER)
 * and Farmer-specific registration fields (FR-02: national ID, district, sector, cooperative).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequest {

    @NotBlank(message = "{validation.name.required}")
    @Size(min = 2, max = 150, message = "{validation.name.size}")
    private String fullName;

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    private String email;

    @NotBlank(message = "{validation.phone.required}")
    @Pattern(regexp = "^(078|079|072|073)\\d{7}$", message = "{validation.phone.invalid}")
    private String phone;

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 8, message = "{validation.password.size}")
    private String password;

    @NotNull(message = "{validation.role.required}")
    private Role role;

    // FR-02: Farmer-specific fields
    private String nationalId;
    private String district;
    private String sector;
    private Long cooperativeId;
}
