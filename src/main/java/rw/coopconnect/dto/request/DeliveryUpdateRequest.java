package rw.coopconnect.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import rw.coopconnect.domain.enums.DeliveryStatus;

/**
 * FR-08: Delivery status update request.
 */
@Getter
@Setter
public class DeliveryUpdateRequest {

    @NotNull(message = "Delivery status is required.")
    private DeliveryStatus status;

    private String notes;
}
