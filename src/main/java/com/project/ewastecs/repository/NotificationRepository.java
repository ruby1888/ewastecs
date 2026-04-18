package com.project.ewastecs.repository;

import com.project.ewastecs.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(
        String recipientType, Long recipientId);

    List<Notification> findByRecipientTypeAndRecipientIdAndReadFalse(
        String recipientType, Long recipientId);

    long countByRecipientTypeAndRecipientIdAndReadFalse(
        String recipientType, Long recipientId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.recipientType = :type AND n.recipientId = :id")
    void deleteAllByRecipientTypeAndId(@Param("type") String type, @Param("id") Long id);
}
