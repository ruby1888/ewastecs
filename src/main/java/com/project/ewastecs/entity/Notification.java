package com.project.ewastecs.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data @NoArgsConstructor @AllArgsConstructor
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "CLIENT" or "AGENT" — whose notification this is
    @Column(nullable = false, length = 10)
    private String recipientType;

    // The ID of the recipient (client.id or agent.id)
    @Column(nullable = false)
    private Long recipientId;

    @Column(nullable = false, length = 300)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Convenience factory
    public static Notification forClient(Long clientId, String message) {
        Notification n = new Notification();
        n.recipientType = "CLIENT";
        n.recipientId = clientId;
        n.message = message;
        return n;
    }

    public static Notification forAgent(Long agentId, String message) {
        Notification n = new Notification();
        n.recipientType = "AGENT";
        n.recipientId = agentId;
        n.message = message;
        return n;
    }
}
