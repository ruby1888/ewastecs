package com.project.ewastecs.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "client_ewastes")
@Data @NoArgsConstructor @AllArgsConstructor
public class Clientewaste {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ewaste_id", nullable = false)
    private Ewaste ewaste;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "item_description", length = 500)
    private String itemDescription;

    @Column(name = "item_image_path", length = 255)
    private String itemImagePath;

    @Column(name = "pickup_address", length = 255)
    private String pickupAddress;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "certificate_issued")
    private boolean certificateIssued = false;
}
