package com.project.ewastecs.repository;
import com.project.ewastecs.entity.Ewaste;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EwasteRepository extends JpaRepository<Ewaste, Long> {
    List<Ewaste> findByCategoryId(Long categoryId);
}
