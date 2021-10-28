package ru.dexsys.mortgageapplicationservice.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CalculateResponse {

    private Double creditAmount;
    private Integer durationInMonths;
    private Double monthlyPayment;

    public CalculateResponse(Double creditAmount, Integer durationInMonths) {
        this.creditAmount = creditAmount;
        this.durationInMonths = durationInMonths;
    }
}
