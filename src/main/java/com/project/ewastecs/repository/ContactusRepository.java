package com.project.ewastecs.repository;
import com.project.ewastecs.entity.Contactus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ContactusRepository extends JpaRepository<Contactus, Long> {
    List<Contactus> findByRepliedFalse();
}
