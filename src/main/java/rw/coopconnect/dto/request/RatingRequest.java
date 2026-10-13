package rw.coopconnect.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * FR-10: Buyer order rating request.
 */
@Getter
@Setter
public class RatingRequest {

    @NotNull(message = "Order ID is required.")
    private Long orderId;

    @NotNull(message = "Score is required.")
    @Min(value = 1, message = "Rating must be at least 1 star.")
    @Max(value = 5, message = "Rating cannot exceed 5 stars.")
    private Integer score;

    private String comment;
}
