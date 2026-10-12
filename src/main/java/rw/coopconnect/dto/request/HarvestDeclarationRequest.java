package rw.coopconnect.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import rw.coopconnect.domain.enums.QualityGrade;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * FR-03: Harvest declaration request by farmer.
 */
@Getter
@Setter
public class HarvestDeclarationRequest {

    @NotNull(message = "Category is required.")
    private Long categoryId;

    @NotNull(message = "Quantity is required.")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than zero.")
    private BigDecimal quantity;

    @NotBlank(message = "Unit of measurement is required.")
    private String unit = "kg";

    @NotNull(message = "Expected harvest date is required.")
    private LocalDate expectedDate;

    @NotNull(message = "Quality grade is required.")
    private QualityGrade qualityGrade = QualityGrade.GRADE_A;
}
