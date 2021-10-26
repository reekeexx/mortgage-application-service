package ru.dexsys.mortgageapplicationservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.dexsys.mortgageapplicationservice.entity.Client;
import ru.dexsys.mortgageapplicationservice.service.ClientService;

import javax.validation.Valid;
import java.util.Collections;
import java.util.Optional;

@RestController
@RequestMapping("/mortgage")
public class ClientController {

    private final ClientService clientService;
    private final RestTemplate restTemplate;
    private final String URI_CALCULATE_SERVICE = "https://mortgage-calculator-service.herokuapp.com/calculate";

    @Autowired
    public ClientController(ClientService clientService, RestTemplateBuilder builder) {
        this.clientService = clientService;
        this.restTemplate = builder.build();
    }

    @PostMapping("/application")
    public ResponseEntity<?> createMortgageApplication(@Valid @RequestBody Client client,
                                                       BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", bindingResult.getFieldError().getDefaultMessage()));
        }

        Optional<Client> duplicateClient = clientService.findClientDuplicate(client.getFirstName(),
                client.getSecondName(), client.getLastName(), client.getPassport());
        if (duplicateClient.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Collections.singletonMap("error", "Client duplicate"));
        }

        double monthlyPayment = 0;

        ResponseEntity<Client> clientResponseEntity = restTemplate.postForEntity(URI_CALCULATE_SERVICE, client, Client.class);
        if (clientResponseEntity.getStatusCode().equals(HttpStatus.OK) && clientResponseEntity.getBody() != null) {
            monthlyPayment = clientResponseEntity.getBody().getMonthlyPayment();
        }

        if (client.getSalary() > monthlyPayment * 2) {
            client.setStatus(Client.MortgageApplicationStatus.APPROVED);
            client.setMonthlyPayment(monthlyPayment);
        } else {
            client.setStatus(Client.MortgageApplicationStatus.DENIED);
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
        Optional<Client> savedClient = clientService.findClientById(id);
        if (savedClient.isPresent()) {
            return ResponseEntity.ok(savedClient.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
