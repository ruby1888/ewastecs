package com.project.ewastecs.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agents")
@Data @NoArgsConstructor @AllArgsConstructor
public class Agent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 15)
    private String mobile;

    @Column(length = 255)
    private String address;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "city_id")
    private City city;

    @Column(name = "license_number", length = 100)
    private String licenseNumber;

    @Column(name = "license_verified")
    private boolean licenseVerified = false;

    @Column(nullable = false)
    private boolean active = false; // Requires admin approval
}
