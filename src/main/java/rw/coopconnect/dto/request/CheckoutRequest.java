package rw.coopconnect.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * FR-06, FR-07: Checkout and mobile payment request.
 */
@Getter
@Setter
public class CheckoutRequest {

    @NotBlank(message = "Payment provider is required (MTN_MOMO or AIRTEL_MONEY).")
    private String provider = "MTN_MOMO";

    @NotBlank(message = "Phone number is required.")
    @Pattern(regexp = "^(078|079|072|073)\\d{7}$", message = "Phone must be a valid Rwandan mobile number (078/079/072/073 followed by 7 digits).")
    private String phoneNumber;

    private String deliveryAddress;
    private String deliveryDistrict;
    private String deliverySector;
}
