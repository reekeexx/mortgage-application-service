package ru.dexsys.mortgageapplicationservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.dexsys.mortgageapplicationservice.entity.Client;
import ru.dexsys.mortgageapplicationservice.model.CalculateResponse;
import ru.dexsys.mortgageapplicationservice.service.ClientService;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

@Tag(name = "Client", description = "Client API")
@RestController
@RequestMapping("/mortgage")
public class ClientController {

    private final ClientService clientService;
    private final RestTemplate restTemplate;

    @Autowired
    public ClientController(ClientService clientService, RestTemplateBuilder builder) {
        this.clientService = clientService;
        this.restTemplate = builder.build();
    }

    @Operation(
            operationId = "createMortgageApplication",
            summary = "Оформить заявку на ипотеку",
            description = "Создает заявку для ипотеки",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Created",
                            content = {
                                    @Content(
                                            mediaType = "application/json",
                                            examples = {
                                                    @ExampleObject(
                                                            value = "{\n" +
                                                                    "  \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
                                                                    "  \"firstName\": \"Иван\",\n" +
                                                                    "  \"secondName\": \"Иванович\",\n" +
                                                                    "  \"lastName\": \"Иванов\",\n" +
                                                                    "  \"passport\": \"9410123456\",\n" +
                                                                    "  \"birthDate\": \"1990-10-23\",\n" +
                                                                    "  \"gender\": \"MALE\",\n" +
                                                                    "  \"salary\": 80000,\n" +
                                                                    "  \"creditAmount\": 3000000,\n" +
                                                                    "  \"durationInMonths\": 120,\n" +
                                                                    "  \"status\": \"PROCESSING\"\n" +
                                                                    "}"
                                                    )
                                            }
                                    )
                            }
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad Request",
                            content = {
                                    @Content(
                                            mediaType = "application/json",
                                            examples = {
                                                    @ExampleObject(
                                                            value = "{\n" +
                                                                    "  \"error\": \"message\"\n" +
                                                                    "}"
                                                    )
                                            }
                                    )
                            }
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Duplicate application",
                            content = {
                                    @Content(
                                            mediaType = "application/json",
                                            examples = {
                                                    @ExampleObject(
                                                            value = "{\n" +
                                                                    "  \"error\": \"Client duplicate\"\n" +
                                                                    "}"
                                                    )
                                            }
                                    )
                            }
                    )
            }
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = {
                    @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            value = "{\n" +
                                                    "  \"firstName\": \"Иван\",\n" +
                                                    "  \"secondName\": \"Иванович\",\n" +
                                                    "  \"lastName\": \"Иванов\",\n" +
                                                    "  \"passport\": \"9410123456\",\n" +
                                                    "  \"birthDate\": \"1990-10-23\",\n" +
                                                    "  \"gender\": \"MALE\",\n" +
                                                    "  \"salary\": 80000,\n" +
                                                    "  \"creditAmount\": 3000000,\n" +
                                                    "  \"durationInMonths\": 120\n" +
                                                    "}"
                                    )
                            }
                    )
            }
    )
    @PostMapping("/application")
    public ResponseEntity<?> createMortgageApplication(@Valid @RequestBody Client client,
                                                       BindingResult bindingResult) {

        if (bindingResult.hasFieldErrors() && bindingResult.getFieldError() != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", bindingResult.getFieldError().getDefaultMessage()));
        }

        Optional<Client> duplicateClient = clientService.findClientDuplicate(client.getFirstName(),
                client.getSecondName(), client.getLastName(), client.getPassport());
        if (duplicateClient.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Collections.singletonMap("error", "Client duplicate"));
        }

        ResponseEntity<CalculateResponse> calculateResponseEntity =
                restTemplate.postForEntity("https://mortgage-calculator-service.herokuapp.com/calculate",
                        new CalculateResponse(client.getCreditAmount(), client.getDurationInMonths()),
                        CalculateResponse.class);

        if (calculateResponseEntity.getStatusCode().equals(HttpStatus.OK) && calculateResponseEntity.getBody() != null) {
            BigDecimal monthlyPayment = calculateResponseEntity.getBody().getMonthlyPayment();
            if (client.getSalary().compareTo(monthlyPayment.multiply(BigDecimal.valueOf(2))) > 0) {
                client.setStatus(Client.MortgageApplicationStatus.APPROVED);
                client.setMonthlyPayment(monthlyPayment);
            } else {
                client.setStatus(Client.MortgageApplicationStatus.DENIED);
            }
        }

        clientService.saveClient(client);

        client.setStatus(Client.MortgageApplicationStatus.PROCESSING);
        client.setMonthlyPayment(null);

        return ResponseEntity.created(
                ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                        .build(Collections.singletonMap("id", client.getId()))
        ).body(client);
    }

    @Operation(
            operationId = "getClientById",
            summary = "Поиск клиента по его ID",
            description = "Возвращает клиента по его ID",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "OK",
                            content = {
                                    @Content(
                                            mediaType = "application/json",
                                            examples = {
                                                    @ExampleObject(
                                                            value = "{\n" +
                                                                    "  \"id\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\n" +
                                                                    "  \"firstName\": \"Иван\",\n" +
                                                                    "  \"secondName\": \"Иванович\",\n" +
                                                                    "  \"lastName\": \"Иванов\",\n" +
                                                                    "  \"passport\": \"9410123456\",\n" +
                                                                    "  \"birthDate\": \"1990-10-23\",\n" +
                                                                    "  \"gender\": \"MALE\",\n" +
                                                                    "  \"salary\": 80000,\n" +
                                                                    "  \"creditAmount\": 3000000,\n" +
                                                                    "  \"durationInMonths\": 120,\n" +
                                                                    "  \"status\": \"APPROVED\",\n" +
                                                                    "  \"monthlyPayment\": 35610.53\n" +
                                                                    "}"
                                                    )
                                            }
                                    )
                            }
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad Request",
                            content = {
                                    @Content(
                                            mediaType = "application/json",
                                            examples = {
                                                    @ExampleObject(
                                                            value = "{\n" +
                                                                    "  \"error\": \"message\"\n" +
                                                                    "}"
                                                    )
                                            }
                                    )
                            }
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Not Found",
                            content = {
                                    @Content()
                            }
                    )
            }
    )
    @GetMapping("/application/{id}")
    public ResponseEntity<?> getClientById(@PathVariable("id") String id) {
        if (id.length() != 36) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", "invalid id"));
        }

        Optional<Client> savedClient = clientService.findClientById(id);
        if (savedClient.isPresent()) {
            return ResponseEntity.ok(savedClient.get());
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleException(HttpMessageNotReadableException e) {
        String localDateException = "java.time.LocalDate";
        String enumGenderException = "ru.dexsys.mortgageapplicationservice.entity.Client$Gender";
        if (e.getMessage().toLowerCase().contains(localDateException.toLowerCase())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", "date format yyyy-mm-dd, example 1999-01-21"));
        } else if (e.getMessage().toLowerCase().contains(enumGenderException.toLowerCase())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", "gender should be MALE or FEMALE"));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Collections.singletonMap("error", e.getMessage()));
    }
}
