package com.project.ewastecs.repository;
import com.project.ewastecs.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CitiesRepository extends JpaRepository<City, Long> {
    List<City> findByStateId(Long stateId);
}
