package ru.dexsys.mortgageapplicationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.dexsys.mortgageapplicationservice.entity.Client;

import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    Optional<Client> findByFirstNameAndSecondNameAndLastNameAndPassport(String firstName, String secondName, String lastName, String passport);
}
