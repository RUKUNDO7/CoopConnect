package rw.coopconnect.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * FR-11: Price dashboard projection response.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProducePriceStat {
    private String cropName;
    private String district;
    private BigDecimal averagePrice;
}
