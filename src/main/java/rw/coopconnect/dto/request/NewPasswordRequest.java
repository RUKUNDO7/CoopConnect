package rw.coopconnect.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FR-01: Setting new password using reset token.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewPasswordRequest {

    @NotBlank
    private String token;

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 8, message = "{validation.password.size}")
    private String newPassword;

    @NotBlank(message = "{validation.password.confirm.required}")
    private String confirmPassword;
}
