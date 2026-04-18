package com.project.ewastecs.repository;

import com.project.ewastecs.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ClientsRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByEmail(String email);
}
