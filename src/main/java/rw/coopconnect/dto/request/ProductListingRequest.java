package rw.coopconnect.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * FR-04: Product listing creation/update by cooperative manager.
 */
@Getter
@Setter
public class ProductListingRequest {

    @NotNull(message = "Verified harvest must be selected.")
    private Long harvestId;

    @NotBlank(message = "Title is required.")
    private String title;

    @NotNull(message = "Price per unit is required.")
    @DecimalMin(value = "1.0", message = "Price per unit must be at least 1 RWF.")
    private BigDecimal pricePerUnit;

    @NotNull(message = "Available quantity is required.")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than zero.")
    private BigDecimal availableQuantity;

    @NotBlank(message = "Unit is required.")
    private String unit = "kg";

    private String photoUrl;
    private boolean active = true;
}
