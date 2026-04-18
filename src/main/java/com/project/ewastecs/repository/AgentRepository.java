package com.project.ewastecs.repository;

import com.project.ewastecs.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface AgentRepository extends JpaRepository<Agent, Long> {
    Optional<Agent> findByEmail(String email);
    List<Agent> findByActiveTrue();
    List<Agent> findByCityId(Long cityId);
}
