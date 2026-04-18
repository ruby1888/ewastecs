package com.project.ewastecs.controller;

import com.project.ewastecs.repository.CitiesRepository;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final CitiesRepository cityRepo;

    public ApiController(CitiesRepository cityRepo) {
        this.cityRepo = cityRepo;
    }

    /** Returns [{id, name}] for cities in a given state */
    @GetMapping("/cities")
    public List<Map<String, Object>> getCitiesByState(@RequestParam Long stateId) {
        return cityRepo.findByStateId(stateId).stream()
            .map(c -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",   c.getId());
                m.put("name", c.getName());
                return m;
            })
            .toList();
    }
}
