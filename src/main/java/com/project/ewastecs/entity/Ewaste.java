package com.project.ewastecs.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "ewastes")
@Data @NoArgsConstructor @AllArgsConstructor
public class Ewaste {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    private String description;

    @Column(name = "price_per_kg")
    private Double pricePerKg;

    @Column(name = "image_path")
    private String imagePath;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;
}
