package rw.coopconnect.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * FR-06: Add item to shopping cart request.
 */
@Getter
@Setter
public class AddToCartRequest {

    @NotNull(message = "Product listing ID is required.")
    private Long listingId;

    @NotNull(message = "Quantity is required.")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than zero.")
    private BigDecimal quantity;
}
