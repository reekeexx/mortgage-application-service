package ru.dexsys.mortgageapplicationservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.dexsys.mortgageapplicationservice.entity.Client;
import ru.dexsys.mortgageapplicationservice.repository.ClientRepository;

import java.util.Optional;
import java.util.UUID;

@Service
public class ClientService {

    private ClientRepository clientRepository;

    @Autowired
    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public Client saveClient(Client client) {
        return clientRepository.save(client);
    }

    public Optional<Client> findClientById(String id) {
        return clientRepository.findById(UUID.fromString(id));
    }

    public Optional<Client> findClientDuplicate(String firstName, String secondName, String lastName, String passport) {
        return clientRepository.findByFirstNameAndSecondNameAndLastNameAndPassport(firstName, secondName, lastName, passport);
    }
}
