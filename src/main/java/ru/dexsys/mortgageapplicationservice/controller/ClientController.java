package ru.dexsys.mortgageapplicationservice.controller;

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
import java.util.Collections;
import java.util.Optional;

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
            double monthlyPayment = calculateResponseEntity.getBody().getMonthlyPayment();
            if (client.getSalary() > monthlyPayment * 2) {
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
