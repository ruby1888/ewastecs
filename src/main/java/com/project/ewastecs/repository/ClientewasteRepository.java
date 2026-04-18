package com.project.ewastecs.repository;
import com.project.ewastecs.entity.Clientewaste;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ClientewasteRepository extends JpaRepository<Clientewaste, Long> {
    List<Clientewaste> findByClientId(Long clientId);
    List<Clientewaste> findByStatus(String status);
    long countByStatus(String status);
}
