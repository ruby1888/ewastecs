package com.project.ewastecs.controller;

import com.project.ewastecs.entity.Contactus;
import com.project.ewastecs.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    private final ContactusRepository contactRepo;
    private final EwasteRepository ewasteRepo;
    private final CitiesRepository cityRepo;
    private final ClientsRepository clientRepo;
    private final ClientewasteRepository clientEwasteRepo;

    public HomeController(ContactusRepository contactRepo,
                          EwasteRepository ewasteRepo,
                          CitiesRepository cityRepo,
                          ClientsRepository clientRepo,
                          ClientewasteRepository clientEwasteRepo) {
        this.contactRepo = contactRepo;
        this.ewasteRepo = ewasteRepo;
        this.cityRepo = cityRepo;
        this.clientRepo = clientRepo;
        this.clientEwasteRepo = clientEwasteRepo;
    }

    @GetMapping({"/", "/index"})
    public String home(Model model) {
        model.addAttribute("ewastes", ewasteRepo.findAll().stream().limit(8).toList());
        model.addAttribute("totalItems",     ewasteRepo.count());
        model.addAttribute("totalCities",    cityRepo.count());
        model.addAttribute("totalClients",   clientRepo.count());
        model.addAttribute("totalCompleted", clientEwasteRepo.countByStatus("COMPLETED"));
        return "index";
    }

    @GetMapping("/about")
    public String about() { return "about"; }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("contactForm", new Contactus());
        return "contact";
    }

    @PostMapping("/contact/save")
    public String saveContact(@ModelAttribute("contactForm") Contactus contactus,
                              RedirectAttributes ra) {
        contactRepo.save(contactus);
        ra.addFlashAttribute("success", "Thank you! We will get back to you shortly.");
        return "redirect:/contact";
    }

    @GetMapping("/access-denied")
    public String accessDenied() { return "AccessDenied"; }
}
