package com.project.ewastecs.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity @Table(name = "client_ewaste_agents")
@Data @NoArgsConstructor @AllArgsConstructor
public class Clientewasteagent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_ewaste_id", nullable = false)
    private Clientewaste clientEwaste;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt = LocalDateTime.now();

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @Column(name = "pickup_date")
    private LocalDate pickupDate;

    @Column(name = "receiver_name", length = 100)
    private String receiverName;

    @Column(name = "receiver_contact", length = 20)
    private String receiverContact;

    @Column(name = "offered_price")
    private Double offeredPrice;

    @Column(name = "free_collection")
    private boolean freeCollection = false;

    @Column(length = 500)
    private String remarks;

    /**
     * Offer status lifecycle:
     *   INVITED   – Admin invited this agent to make a price offer (pickup still PENDING)
     *   OFFERED   – Agent submitted a price offer (awaiting client acceptance)
     *   ACTIVE    – Client accepted this agent's offer → agent is now collecting
     *   REJECTED  – Client chose a different agent's offer
     */
    @Column(name = "offer_status", length = 20)
    private String offerStatus = "ACTIVE";
}
