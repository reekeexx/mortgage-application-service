package ru.dexsys.mortgageapplicationservice.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Table(name = "clients")
public class Client {

    @Id
    @Column(name = "id")
    private UUID id = UUID.randomUUID();

    @Column(name = "first_name")
    @NotBlank(message = "firstName cannot be empty")
    private String firstName;

    @Column(name = "second_name")
    @NotBlank(message = "secondName cannot be empty")
    private String secondName;

    @Column(name = "last_name")
    @NotBlank(message = "lastName cannot be empty")
    private String lastName;

    @Column(name = "passport")
    @NotBlank(message = "passport cannot be empty")
    private String passport;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "gender")
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "salary")
    @NotNull(message = "salary cannot be null")
    @Min(value = 0, message = "salary min value = 0")
    @Max(value = 1000000000, message = "salary max value = 1000000000")
    private Double salary;

    @Column(name = "credit_amount")
    @NotNull(message = "creditAmount cannot be null")
    @Min(value = 0, message = "creditAmount min value = 0")
    @Max(value = 1000000000, message = "creditAmount max value = 1000000000")
    private Double creditAmount;

    @Column(name = "duration_in_months")
    @NotNull(message = "durationInMonths cannot be null")
    @Min(value = 0, message = "durationInMonths min value = 0")
    @Max(value = 1200, message = "durationInMonths max value = 1200")
    private Integer durationInMonths;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private MortgageApplicationStatus status;

    @Column(name = "monthly_payment")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double monthlyPayment;

    public enum Gender {
        MALE, FEMALE
    }

    public enum MortgageApplicationStatus {
        PROCESSING, APPROVED, DENIED
    }
}
