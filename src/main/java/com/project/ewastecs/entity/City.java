package com.project.ewastecs.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "cities")
@Data @NoArgsConstructor @AllArgsConstructor
public class City {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "state_id")
    private State state;
}
