package com.project.ewastecs.repository;

import com.project.ewastecs.entity.Clientewasteagent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClientewasteagentRepository extends JpaRepository<Clientewasteagent, Long> {

    /** All records for an agent (any status) */
    List<Clientewasteagent> findByAgentId(Long agentId);

    /** Records for an agent filtered by status */
    List<Clientewasteagent> findByAgentIdAndOfferStatus(Long agentId, String offerStatus);

    /** All records for a given pickup (any status) */
    List<Clientewasteagent> findAllByClientEwasteId(Long clientEwasteId);

    /** Single record for pickup + status (e.g. find the ACTIVE assignment) */
    Optional<Clientewasteagent> findByClientEwasteIdAndOfferStatus(Long clientEwasteId, String offerStatus);

    /** Legacy – first record found for pickup */
    Optional<Clientewasteagent> findByClientEwasteId(Long clientEwasteId);

    /** Check if agent already has any record for a pickup */
    boolean existsByClientEwasteIdAndAgentId(Long clientEwasteId, Long agentId);
}
