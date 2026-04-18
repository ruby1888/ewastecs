package com.project.ewastecs.controller;

import com.project.ewastecs.CommonFuns;
import com.project.ewastecs.entity.*;
import com.project.ewastecs.repository.*;
import com.project.ewastecs.service.NotificationService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/client")
public class ClientController {

    private final ClientsRepository clientRepo;
    private final EwasteRepository ewasteRepo;
    private final ClientewasteRepository clientEwasteRepo;
    private final ClientewasteagentRepository assignmentRepo;
    private final PasswordEncoder encoder;
    private final NotificationService notifService;
    private final StateRepository stateRepo;
    private final CommonFuns commonFuns;

    public ClientController(ClientsRepository clientRepo, EwasteRepository ewasteRepo,
                            ClientewasteRepository clientEwasteRepo,
                            ClientewasteagentRepository assignmentRepo,
                            PasswordEncoder encoder, NotificationService notifService,
                            StateRepository stateRepo, CommonFuns commonFuns) {
        this.clientRepo = clientRepo; this.ewasteRepo = ewasteRepo;
        this.clientEwasteRepo = clientEwasteRepo; this.assignmentRepo = assignmentRepo;
        this.encoder = encoder; this.notifService = notifService;
        this.stateRepo = stateRepo; this.commonFuns = commonFuns;
    }

    @GetMapping("/login")
    public String loginPage() { return "client/login"; }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("client", new Client());
        return "client/register";
    }

    @PostMapping("/register/save")
    public String register(@ModelAttribute Client client, RedirectAttributes ra) {
        if (clientRepo.findByEmail(client.getEmail()).isPresent()) {
            ra.addFlashAttribute("error", "Email already registered.");
            return "redirect:/client/register";
        }
        client.setPassword(encoder.encode(client.getPassword()));
        client.setActive(true);
        clientRepo.save(client);
        ra.addFlashAttribute("success", "Registration successful! Please login.");
        return "redirect:/client/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        Client client = getClient(principal);
        var requests = clientEwasteRepo.findByClientId(client.getId());
        var notifications = notifService.getClientNotifications(client.getId());
        notifService.markAllClientRead(client.getId());

        long pendingCount   = requests.stream().filter(r -> "PENDING".equals(r.getStatus())).count();
        long assignedCount  = requests.stream().filter(r -> "ASSIGNED".equals(r.getStatus())).count();
        long completedCount = requests.stream().filter(r -> "COMPLETED".equals(r.getStatus())).count();
        long cancelledCount = requests.stream().filter(r -> "CANCELLED".equals(r.getStatus())).count();

        // Build map: pickupId -> active assignment (for showing agent details in View modal)
        Map<Long, Clientewasteagent> assignmentMap = new HashMap<>();
        // Build map: pickupId -> list of OFFERED assignments (for client to choose from)
        Map<Long, List<Clientewasteagent>> offersMap = new HashMap<>();
        for (var req : requests) {
            var all = assignmentRepo.findAllByClientEwasteId(req.getId());
            // Active assignment: ACTIVE status OR legacy null (rows created before offer_status column)
            all.stream()
               .filter(a -> "ACTIVE".equals(a.getOfferStatus()) || a.getOfferStatus() == null)
               .findFirst()
               .ifPresent(a -> assignmentMap.put(req.getId(), a));
            // Pending offers (OFFERED only - not INVITED which haven't priced yet)
            var offered = all.stream().filter(a -> "OFFERED".equals(a.getOfferStatus())).toList();
            if (!offered.isEmpty()) offersMap.put(req.getId(), offered);
        }

        model.addAttribute("client", client);
        model.addAttribute("requests", requests);
        model.addAttribute("notifications", notifications);
        model.addAttribute("totalRequests", requests.size());
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("assignedCount", assignedCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("cancelledCount", cancelledCount);
        model.addAttribute("assignmentMap", assignmentMap);
        model.addAttribute("offersMap", offersMap);
        model.addAttribute("chartData", "[" + pendingCount + "," + assignedCount + "," + completedCount + "," + cancelledCount + "]");
        return "client/dashboard";
    }

    /** Client accepts an agent's offer */
    @PostMapping("/accept-offer/{offerId}")
    public String acceptOffer(@PathVariable Long offerId, Principal principal, RedirectAttributes ra) {
        Client client = getClient(principal);
        assignmentRepo.findById(offerId).ifPresent(offer -> {
            if (!offer.getClientEwaste().getClient().getId().equals(client.getId())) return;
            if (!"OFFERED".equals(offer.getOfferStatus())) return;

            Long pickupId = offer.getClientEwaste().getId();

            // Reject all other OFFERED assignments for this pickup
            assignmentRepo.findAllByClientEwasteId(pickupId).stream()
                .filter(a -> "OFFERED".equals(a.getOfferStatus()) && !a.getId().equals(offerId))
                .forEach(a -> { a.setOfferStatus("REJECTED"); assignmentRepo.save(a); });

            // Accept this one
            offer.setOfferStatus("ACTIVE");
            assignmentRepo.save(offer);

            // Update pickup status
            Clientewaste pickup = offer.getClientEwaste();
            pickup.setStatus("ASSIGNED");
            clientEwasteRepo.save(pickup);

            // Notify agent
            notifService.notifyAgent(offer.getAgent().getId(),
                client.getName() + " accepted your offer for \"" + pickup.getEwaste().getName() + "\". Pickup is confirmed!");
            notifService.notifyAdmin("Client " + client.getName() + " accepted offer from agent " + 
                offer.getAgent().getName() + " for pickup #" + offer.getClientEwaste().getId());
            ra.addFlashAttribute("success", "You have accepted the offer from " + offer.getAgent().getName() + ". They will contact you soon.");
        });
        return "redirect:/client/dashboard";
    }

    @GetMapping("/add-ewaste")
    public String addEwastePage(Model model) {
        model.addAttribute("ewastes", ewasteRepo.findAll());
        model.addAttribute("states", stateRepo.findAll());
        return "client/addewaste";
    }

    @PostMapping("/add-ewaste/save")
    public String savePickup(Principal principal,
                             @RequestParam Long ewasteId, @RequestParam Integer quantity,
                             @RequestParam(required = false) String itemDescription,
                             @RequestParam String pickupAddress,
                             @RequestParam(value = "itemImage", required = false) MultipartFile itemImage,
                             RedirectAttributes ra) {
        Client client = getClient(principal);
        ewasteRepo.findById(ewasteId).ifPresent(ewaste -> {
            Clientewaste req = new Clientewaste();
            req.setClient(client); req.setEwaste(ewaste);
            req.setQuantity(quantity); req.setItemDescription(itemDescription);
            req.setPickupAddress(pickupAddress);
            req.setRequestedAt(LocalDateTime.now());
            req.setStatus("PENDING");
            if (itemImage != null && !itemImage.isEmpty()) {
                try { req.setItemImagePath(commonFuns.saveFile(itemImage)); }
                catch (Exception e) { System.err.println("Image save failed: " + e.getMessage()); }
            }
            clientEwasteRepo.save(req);
            notifService.notifyClient(client.getId(),
                "Pickup for \"" + ewaste.getName() + "\" (" + quantity + " units) submitted.");
            notifService.notifyAdmin("New pickup request from " + client.getName() + 
                " for " + ewaste.getName() + " (" + quantity + " units).");
        });
        ra.addFlashAttribute("success", "Pickup request submitted!");
        return "redirect:/client/dashboard";
    }

    @PostMapping("/notifications/clear")
    public String clearNotifications(Principal principal, RedirectAttributes ra) {
        Client client = getClient(principal);
        notifService.markAllClientRead(client.getId());
        ra.addFlashAttribute("success", "Notifications cleared.");
        return "redirect:/client/dashboard";
    }

    @GetMapping("/certificate/{id}")
    public String viewCertificate(@PathVariable Long id, Principal principal, Model model) {
        Client client = getClient(principal);
        Clientewaste pickup = clientEwasteRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Request not found"));
        if (!pickup.getClient().getId().equals(client.getId())) return "redirect:/client/dashboard";
        if (!"COMPLETED".equals(pickup.getStatus())) return "redirect:/client/my-certificates";
        assignmentRepo.findByClientEwasteIdAndOfferStatus(id, "ACTIVE")
            .ifPresent(a -> model.addAttribute("assignment", a));
        model.addAttribute("pickup", pickup);
        model.addAttribute("client", client);
        return "client/certificate";
    }

    @GetMapping("/my-certificates")
    public String myCertificates(Principal principal, Model model) {
        Client client = getClient(principal);
        var completed = clientEwasteRepo.findByClientId(client.getId()).stream()
            .filter(r -> "COMPLETED".equals(r.getStatus())).toList();
        model.addAttribute("client", client);
        model.addAttribute("completedRequests", completed);
        return "client/mycertificate";
    }

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        model.addAttribute("client", getClient(principal));
        return "client/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(Principal principal,
                                @RequestParam String name, @RequestParam String mobile,
                                @RequestParam(required = false) String address,
                                RedirectAttributes ra) {
        clientRepo.findByEmail(principal.getName()).ifPresent(c -> {
            c.setName(name); c.setMobile(mobile); c.setAddress(address);
            clientRepo.save(c); ra.addFlashAttribute("success", "Profile updated.");
        });
        return "redirect:/client/profile";
    }

    @PostMapping("/delete-account")
    public String deleteAccount(Principal principal) {
        clientRepo.findByEmail(principal.getName()).ifPresent(c -> { c.setActive(false); clientRepo.save(c); });
        SecurityContextHolder.clearContext();
        return "redirect:/";
    }

    @GetMapping("/change-password")
    public String changePwPage() { return "client/changepassword"; }

    @PostMapping("/change-password")
    public String changePw(Principal principal,
                           @RequestParam String currentPassword, @RequestParam String newPassword,
                           RedirectAttributes ra) {
        clientRepo.findByEmail(principal.getName()).ifPresent(c -> {
            if (encoder.matches(currentPassword, c.getPassword())) {
                c.setPassword(encoder.encode(newPassword)); clientRepo.save(c);
                ra.addFlashAttribute("success", "Password changed.");
            } else ra.addFlashAttribute("error", "Current password incorrect.");
        });
        return "redirect:/client/change-password";
    }

    @PostMapping("/add-ewaste/remove-image/{id}")
    public String removeItemImage(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        Client client = getClient(principal);
        clientEwasteRepo.findById(id).ifPresent(r -> {
            if (r.getClient().getId().equals(client.getId())) {
                commonFuns.deleteFile(r.getItemImagePath());
                r.setItemImagePath(null);
                clientEwasteRepo.save(r);
                ra.addFlashAttribute("success", "Image removed.");
            }
        });
        return "redirect:/client/dashboard";
    }

    private Client getClient(Principal principal) {
        return clientRepo.findByEmail(principal.getName())
            .orElseThrow(() -> new RuntimeException("Client not found"));
    }
}
