package com.project.ewastecs.controller;

import com.project.ewastecs.entity.*;
import com.project.ewastecs.repository.*;
import com.project.ewastecs.service.NotificationService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/agent")
public class AgentController {

    private final AgentRepository agentRepo;
    private final CitiesRepository cityRepo;
    private final StateRepository stateRepo;
    private final ClientewasteagentRepository assignmentRepo;
    private final ClientewasteRepository clientEwasteRepo;
    private final PasswordEncoder encoder;
    private final NotificationService notifService;

    public AgentController(AgentRepository agentRepo, CitiesRepository cityRepo,
                           StateRepository stateRepo,
                           ClientewasteagentRepository assignmentRepo,
                           ClientewasteRepository clientEwasteRepo,
                           PasswordEncoder encoder, NotificationService notifService) {
        this.agentRepo = agentRepo;
        this.cityRepo = cityRepo;
        this.stateRepo = stateRepo;
        this.assignmentRepo = assignmentRepo;
        this.clientEwasteRepo = clientEwasteRepo;
        this.encoder = encoder;
        this.notifService = notifService;
    }

    @GetMapping("/login")
    public String loginPage() { return "agent/login"; }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("agent", new Agent());
        model.addAttribute("cities", cityRepo.findAll());
        model.addAttribute("states", stateRepo.findAll());
        return "agent/register";
    }

    @PostMapping("/register/save")
    public String register(@RequestParam String name, @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam(required = false) String mobile,
                           @RequestParam(required = false) String address,
                           @RequestParam(required = false) Long cityId,
                           @RequestParam(required = false) String licenseNumber,
                           RedirectAttributes ra) {
        if (agentRepo.findByEmail(email).isPresent()) {
            ra.addFlashAttribute("error", "Email already registered.");
            return "redirect:/agent/register";
        }
        if (licenseNumber == null || licenseNumber.isBlank()) {
            ra.addFlashAttribute("error", "A valid collector license number is required to apply.");
            return "redirect:/agent/register";
        }
        Agent agent = new Agent();
        agent.setName(name);
        agent.setEmail(email);
        agent.setPassword(encoder.encode(password));
        agent.setMobile(mobile);
        agent.setAddress(address);
        agent.setActive(false);
        agent.setLicenseNumber(licenseNumber.trim());
        agent.setLicenseVerified(false);
        if (cityId != null) cityRepo.findById(cityId).ifPresent(agent::setCity);
        agentRepo.save(agent);
        notifService.notifyAdmin("New agent application from " + name + " (License: " + licenseNumber.trim() + "). Review in Agents panel.");
        ra.addFlashAttribute("success", "Application submitted! Admin will review your license and activate your account. You will be able to login once approved.");
        return "redirect:/agent/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        Agent agent = getAgent(principal);
        List<Clientewasteagent> assignments = assignmentRepo.findByAgentIdAndOfferStatus(agent.getId(), "ACTIVE");
        List<Clientewasteagent> myOffers   = assignmentRepo.findByAgentIdAndOfferStatus(agent.getId(), "OFFERED");
        List<Clientewasteagent> myInvites  = assignmentRepo.findByAgentIdAndOfferStatus(agent.getId(), "INVITED");
        var notifications = notifService.getAgentNotifications(agent.getId());
        notifService.markAllAgentRead(agent.getId());
        long pendingCount   = assignments.stream().filter(a -> "ASSIGNED".equals(a.getClientEwaste().getStatus())).count();
        long collectedCount = assignments.stream().filter(a -> "COMPLETED".equals(a.getClientEwaste().getStatus())).count();
        model.addAttribute("agent", agent);
        model.addAttribute("assignments", assignments);
        model.addAttribute("myOffers", myOffers);
        model.addAttribute("myInvites", myInvites);
        model.addAttribute("inviteCount", myInvites.size());
        model.addAttribute("notifications", notifications);
        model.addAttribute("totalAssigned", assignments.size());
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("collectedCount", collectedCount);
        return "agent/dashboard";
    }

    @GetMapping("/pickups")
    public String myPickups(Principal principal, Model model, RedirectAttributes ra) {
        Agent agent = getAgent(principal);
        if (!hasLicense(agent)) {
            ra.addFlashAttribute("licenseRequired", "Please add your collector license number on your profile before viewing pickups.");
            return "redirect:/agent/profile";
        }
        List<Clientewasteagent> assignments = assignmentRepo.findByAgentIdAndOfferStatus(agent.getId(), "ACTIVE");
        long pendingCount   = assignments.stream().filter(a -> "ASSIGNED".equals(a.getClientEwaste().getStatus())).count();
        long collectedCount = assignments.stream().filter(a -> "COMPLETED".equals(a.getClientEwaste().getStatus())).count();
        model.addAttribute("agent", agent);
        model.addAttribute("assignments", assignments);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("collectedCount", collectedCount);
        return "agent/pickups";
    }

    @GetMapping("/pickup-requests")
    public String browseRequests(Principal principal, Model model, RedirectAttributes ra) {
        Agent agent = getAgent(principal);
        if (!hasLicense(agent)) {
            ra.addFlashAttribute("licenseRequired", "Please add your collector license number on your profile before browsing or accepting pickup requests.");
            return "redirect:/agent/profile";
        }
        // Only show pickups where admin specifically invited this agent (INVITED status)
        // Agents cannot self-browse and self-offer — admin controls assignments
        List<Clientewasteagent> invitations = assignmentRepo.findByAgentIdAndOfferStatus(agent.getId(), "INVITED");
        // Also show OFFERED pickups (ones agent already made offer on, still awaiting client)
        List<Clientewasteagent> myOffered  = assignmentRepo.findByAgentIdAndOfferStatus(agent.getId(), "OFFERED");

        model.addAttribute("agent", agent);
        model.addAttribute("invitations", invitations);
        model.addAttribute("myOffered", myOffered);
        return "agent/pickup_requests";
    }

    /**
     * Agent submits a price offer.
     * If admin invited the agent (INVITED), converts that record to OFFERED.
     * Otherwise creates a new self-submitted OFFERED record.
     * No lambda used — avoids effectively-final and early-return issues.
     */
    @PostMapping("/pickup-requests/offer")
    public String submitOffer(Principal principal,
                              @RequestParam Long pickupId,
                              @RequestParam(required = false) Double offeredPrice,
                              @RequestParam(required = false) String freeCollection,
                              @RequestParam(required = false) String offerNote,
                              RedirectAttributes ra) {
        Agent agent = getAgent(principal);

        Optional<Clientewaste> pickupOpt = clientEwasteRepo.findById(pickupId);
        if (pickupOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Pickup not found.");
            return "redirect:/agent/pickup-requests";
        }
        Clientewaste pickup = pickupOpt.get();

        if (!hasLicense(agent)) {
            ra.addFlashAttribute("licenseRequired", "You must add a license number to your profile before submitting offers.");
            return "redirect:/agent/profile";
        }
        if (!"PENDING".equals(pickup.getStatus())) {
            ra.addFlashAttribute("error", "This pickup is no longer accepting offers.");
            return "redirect:/agent/pickup-requests";
        }

        boolean isFree = "on".equals(freeCollection) || "true".equals(freeCollection);
        Double price   = isFree ? null : offeredPrice;

        // Check if this agent was INVITED by admin
        Optional<Clientewasteagent> inviteOpt = assignmentRepo.findAllByClientEwasteId(pickupId)
            .stream()
            .filter(a -> a.getAgent().getId().equals(agent.getId()) && "INVITED".equals(a.getOfferStatus()))
            .findFirst();

        if (inviteOpt.isPresent()) {
            // Convert INVITED -> OFFERED (update existing record)
            Clientewasteagent invite = inviteOpt.get();
            invite.setOfferStatus("OFFERED");
            invite.setOfferedPrice(price);
            invite.setFreeCollection(isFree);
            invite.setRemarks(offerNote);
            assignmentRepo.save(invite);
        } else {
            // Self-submitted offer — ensure no duplicate
            boolean alreadyOffered = assignmentRepo.findAllByClientEwasteId(pickupId).stream()
                .anyMatch(a -> a.getAgent().getId().equals(agent.getId())
                    && ("OFFERED".equals(a.getOfferStatus()) || "ACTIVE".equals(a.getOfferStatus())));
            if (alreadyOffered) {
                ra.addFlashAttribute("error", "You have already submitted an offer for this pickup.");
                return "redirect:/agent/pickup-requests";
            }
            Clientewasteagent offer = new Clientewasteagent();
            offer.setClientEwaste(pickup);
            offer.setAgent(agent);
            offer.setAssignedAt(LocalDateTime.now());
            offer.setOfferStatus("OFFERED");
            offer.setFreeCollection(isFree);
            offer.setOfferedPrice(price);
            offer.setRemarks(offerNote);
            assignmentRepo.save(offer);
        }

        // Build human-readable price label
        String priceLabel;
        if (isFree) {
            priceLabel = "free collection";
        } else if (price != null) {
            priceLabel = "Rs." + price + " per item";
        } else {
            priceLabel = "price to be confirmed";
        }

        notifService.notifyClient(pickup.getClient().getId(),
            "Agent " + agent.getName() + " offered " + priceLabel
            + " for your request [" + pickup.getEwaste().getName() + "]. Open your dashboard to accept.");
        notifService.notifyAdmin("Agent " + agent.getName() + " submitted an offer for pickup #"
            + pickupId + " (" + pickup.getEwaste().getName() + ")");

        ra.addFlashAttribute("success", "Your offer has been sent to the client!");
        return "redirect:/agent/pickup-requests";
    }

    @PostMapping("/pickups/update/{assignmentId}")
    public String updatePickup(@PathVariable Long assignmentId,
                               @RequestParam(required = false) String pickupDate,
                               @RequestParam(required = false) String receiverName,
                               @RequestParam(required = false) String receiverContact,
                               @RequestParam(required = false) Double offeredPrice,
                               @RequestParam(required = false) String freeCollection,
                               @RequestParam(required = false) String remarks,
                               RedirectAttributes ra) {
        assignmentRepo.findById(assignmentId).ifPresent(a -> {
            if (pickupDate != null && !pickupDate.isBlank())
                a.setPickupDate(LocalDate.parse(pickupDate));
            a.setReceiverName(receiverName);
            a.setReceiverContact(receiverContact);
            boolean isFree = "on".equals(freeCollection) || "true".equals(freeCollection);
            a.setFreeCollection(isFree);
            a.setOfferedPrice(isFree ? null : offeredPrice);
            a.setRemarks(remarks);
            assignmentRepo.save(a);
            notifService.notifyClient(a.getClientEwaste().getClient().getId(),
                "Agent " + a.getAgent().getName() + " updated pickup details for ["
                + a.getClientEwaste().getEwaste().getName() + "].");
        });
        ra.addFlashAttribute("success", "Pickup details updated.");
        return "redirect:/agent/pickups";
    }

    @PostMapping("/pickups/collect/{assignmentId}")
    public String markCollected(@PathVariable Long assignmentId, RedirectAttributes ra) {
        assignmentRepo.findById(assignmentId).ifPresent(a -> {
            a.setCollectedAt(LocalDateTime.now());
            assignmentRepo.save(a);
            Clientewaste pickup = a.getClientEwaste();
            pickup.setStatus("COMPLETED");
            pickup.setCertificateIssued(true);
            if (a.getOfferedPrice() != null)
                pickup.setTotalAmount(a.getOfferedPrice() * pickup.getQuantity());
            clientEwasteRepo.save(pickup);
            notifService.notifyClient(pickup.getClient().getId(),
                "Your e-waste [" + pickup.getEwaste().getName() + "] has been collected by "
                + a.getAgent().getName() + ". Your certificate is ready!");
        });
        ra.addFlashAttribute("success", "Pickup marked as collected. Certificate issued.");
        return "redirect:/agent/pickups";
    }

    @PostMapping("/notifications/clear")
    public String clearNotifications(Principal principal, RedirectAttributes ra) {
        notifService.markAllAgentRead(getAgent(principal).getId());
        ra.addFlashAttribute("success", "Notifications cleared.");
        return "redirect:/agent/dashboard";
    }

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        model.addAttribute("agent", getAgent(principal));
        model.addAttribute("cities", cityRepo.findAll());
        model.addAttribute("states", stateRepo.findAll());
        return "agent/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(Principal principal,
                                @RequestParam String name, @RequestParam String mobile,
                                @RequestParam(required = false) String address,
                                @RequestParam(required = false) String licenseNumber,
                                RedirectAttributes ra) {
        agentRepo.findByEmail(principal.getName()).ifPresent(a -> {
            a.setName(name); a.setMobile(mobile); a.setAddress(address);
            if (licenseNumber != null && !licenseNumber.isBlank()) {
                a.setLicenseNumber(licenseNumber);
                a.setLicenseVerified(false);
            }
            agentRepo.save(a);
            ra.addFlashAttribute("success", "Profile updated.");
        });
        return "redirect:/agent/profile";
    }

    @PostMapping("/delete-account")
    public String deleteAccount(Principal principal) {
        agentRepo.findByEmail(principal.getName()).ifPresent(a -> { a.setActive(false); agentRepo.save(a); });
        SecurityContextHolder.clearContext();
        return "redirect:/";
    }

    @GetMapping("/change-password")
    public String changePwPage() { return "agent/changepassword"; }

    @PostMapping("/change-password")
    public String changePw(Principal principal,
                           @RequestParam String currentPassword,
                           @RequestParam String newPassword,
                           RedirectAttributes ra) {
        agentRepo.findByEmail(principal.getName()).ifPresent(a -> {
            if (encoder.matches(currentPassword, a.getPassword())) {
                a.setPassword(encoder.encode(newPassword));
                agentRepo.save(a);
                ra.addFlashAttribute("success", "Password changed.");
            } else {
                ra.addFlashAttribute("error", "Current password incorrect.");
            }
        });
        return "redirect:/agent/change-password";
    }

    private Agent getAgent(Principal principal) {
        return agentRepo.findByEmail(principal.getName())
            .orElseThrow(() -> new RuntimeException("Agent not found"));
    }

    /**
     * Returns true if the agent is allowed to do pickup operations.
     * An agent must have a license number on file before they can accept/offer on pickups.
     * Admin must also approve the account (active=true) — enforced at login by Spring Security.
     */
    private boolean hasLicense(Agent agent) {
        return agent.getLicenseNumber() != null && !agent.getLicenseNumber().isBlank();
    }
}
