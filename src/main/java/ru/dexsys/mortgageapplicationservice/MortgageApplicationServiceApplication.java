package ru.dexsys.mortgageapplicationservice;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(
        info = @Info(
                title = "Mortgage Application Service",
                description = "Сервис ипотечных заявок",
                version = "1.0"
        )
)
@SpringBootApplication
public class MortgageApplicationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MortgageApplicationServiceApplication.class, args);
    }

}
