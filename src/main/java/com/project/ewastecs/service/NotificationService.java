package com.project.ewastecs.service;

import com.project.ewastecs.entity.Notification;
import com.project.ewastecs.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NotificationService {

    // Admin is stored with recipientType="ADMIN" and recipientId=0
    private static final Long ADMIN_ID = 0L;

    private final NotificationRepository repo;

    public NotificationService(NotificationRepository repo) { this.repo = repo; }

    public void notifyClient(Long clientId, String message) {
        repo.save(Notification.forClient(clientId, message));
    }

    public void notifyAgent(Long agentId, String message) {
        repo.save(Notification.forAgent(agentId, message));
    }

    /** Sends a notification to the admin dashboard */
    public void notifyAdmin(String message) {
        repo.save(Notification.forAgent(ADMIN_ID, message)); // reuse AGENT type with id=0
    }

    public List<Notification> getClientNotifications(Long clientId) {
        return repo.findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc("CLIENT", clientId);
    }

    public List<Notification> getAgentNotifications(Long agentId) {
        return repo.findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc("AGENT", agentId);
    }

    public List<Notification> getAdminNotifications() {
        return repo.findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc("AGENT", ADMIN_ID);
    }

    public long getUnreadClientCount(Long clientId) {
        return repo.countByRecipientTypeAndRecipientIdAndReadFalse("CLIENT", clientId);
    }

    public long getUnreadAgentCount(Long agentId) {
        return repo.countByRecipientTypeAndRecipientIdAndReadFalse("AGENT", agentId);
    }

    public long getUnreadAdminCount() {
        return repo.countByRecipientTypeAndRecipientIdAndReadFalse("AGENT", ADMIN_ID);
    }

    public void markAllClientRead(Long clientId) {
        var notifs = repo.findByRecipientTypeAndRecipientIdAndReadFalse("CLIENT", clientId);
        notifs.forEach(n -> n.setRead(true));
        repo.saveAll(notifs);
    }

    public void markAllAgentRead(Long agentId) {
        var notifs = repo.findByRecipientTypeAndRecipientIdAndReadFalse("AGENT", agentId);
        notifs.forEach(n -> n.setRead(true));
        repo.saveAll(notifs);
    }

    public void markAllAdminRead() {
        markAllAgentRead(ADMIN_ID);
    }
}
