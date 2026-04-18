package com.project.ewastecs.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "categories")
@Data @NoArgsConstructor @AllArgsConstructor
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    private String description;
}
