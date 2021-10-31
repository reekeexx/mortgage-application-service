package ru.dexsys.mortgageapplicationservice.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class CalculateResponse {

    private BigDecimal creditAmount;
    private Integer durationInMonths;
    private BigDecimal monthlyPayment;

    public CalculateResponse(BigDecimal creditAmount, Integer durationInMonths) {
        this.creditAmount = creditAmount;
        this.durationInMonths = durationInMonths;
    }
}
