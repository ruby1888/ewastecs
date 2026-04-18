package com.project.ewastecs.controller;

import com.project.ewastecs.CommonFuns;
import com.project.ewastecs.entity.*;
import com.project.ewastecs.repository.*;
import com.project.ewastecs.service.EmailService;
import com.project.ewastecs.service.NotificationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminRepository adminRepo;
    private final ClientsRepository clientRepo;
    private final AgentRepository agentRepo;
    private final StateRepository stateRepo;
    private final CitiesRepository cityRepo;
    private final CategoryRepository categoryRepo;
    private final EwasteRepository ewasteRepo;
    private final ClientewasteRepository clientEwasteRepo;
    private final ClientewasteagentRepository assignmentRepo;
    private final ContactusRepository contactRepo;
    private final PasswordEncoder encoder;
    private final CommonFuns commonFuns;
    private final NotificationService notifService;
    private final EmailService emailService;

    public AdminController(AdminRepository adminRepo, ClientsRepository clientRepo,
                           AgentRepository agentRepo, StateRepository stateRepo,
                           CitiesRepository cityRepo, CategoryRepository categoryRepo,
                           EwasteRepository ewasteRepo, ClientewasteRepository clientEwasteRepo,
                           ClientewasteagentRepository assignmentRepo,
                           ContactusRepository contactRepo, PasswordEncoder encoder,
                           CommonFuns commonFuns, NotificationService notifService,
                           EmailService emailService) {
        this.adminRepo = adminRepo; this.clientRepo = clientRepo;
        this.agentRepo = agentRepo; this.stateRepo = stateRepo;
        this.cityRepo = cityRepo; this.categoryRepo = categoryRepo;
        this.ewasteRepo = ewasteRepo; this.clientEwasteRepo = clientEwasteRepo;
        this.assignmentRepo = assignmentRepo; this.contactRepo = contactRepo;
        this.encoder = encoder; this.commonFuns = commonFuns;
        this.notifService = notifService; this.emailService = emailService;
    }

    // ─── Login ───────────────────────────────────────────────────────────────
    @GetMapping("/login")
    public String loginPage() { return "admin/login"; }

    // ─── Dashboard ───────────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        long totalClients    = clientRepo.count();
        long totalAgents     = agentRepo.count();
        long pendingPickups  = clientEwasteRepo.countByStatus("PENDING");
        long assignedPickups = clientEwasteRepo.countByStatus("ASSIGNED");
        long completedPickups= clientEwasteRepo.countByStatus("COMPLETED");
        long cancelledPickups= clientEwasteRepo.countByStatus("CANCELLED");
        long pendingContacts = contactRepo.findByRepliedFalse().size();
        long pendingAgents   = agentRepo.findAll().stream().filter(a -> !a.isActive()).count();

        model.addAttribute("totalClients", totalClients);
        model.addAttribute("totalAgents", totalAgents);
        model.addAttribute("pendingPickups", pendingPickups);
        model.addAttribute("assignedPickups", assignedPickups);
        model.addAttribute("completedPickups", completedPickups);
        model.addAttribute("cancelledPickups", cancelledPickups);
        model.addAttribute("pendingContacts", pendingContacts);
        model.addAttribute("pendingAgents", pendingAgents);
        model.addAttribute("recentRequests",
            clientEwasteRepo.findByStatus("PENDING").stream().limit(5).toList());
        model.addAttribute("adminEmail", principal.getName());
        var adminNotifs = notifService.getAdminNotifications();
        notifService.markAllAdminRead();
        model.addAttribute("notifications", adminNotifs);

        // Chart data as JSON strings
        model.addAttribute("chartPickupData",
            "[" + pendingPickups + "," + assignedPickups + "," + completedPickups + "," + cancelledPickups + "]");
        model.addAttribute("chartUserData",
            "[" + totalClients + "," + totalAgents + "]");

        return "admin/dashboard";
    }

    // ─── States ──────────────────────────────────────────────────────────────
    @GetMapping("/states")
    public String states(Model model) {
        model.addAttribute("states", stateRepo.findAll());
        model.addAttribute("state", new State());
        return "admin/state";
    }

    @GetMapping("/states/edit/{id}")
    public String editState(@PathVariable Long id, Model model) {
        model.addAttribute("states", stateRepo.findAll());
        model.addAttribute("state", stateRepo.findById(id).orElse(new State()));
        return "admin/state";
    }

    @PostMapping("/states/save")
    public String saveState(@ModelAttribute State state, RedirectAttributes ra) {
        stateRepo.save(state);
        ra.addFlashAttribute("success", "State saved.");
        return "redirect:/admin/states";
    }

    @GetMapping("/states/delete/{id}")
    public String deleteState(@PathVariable Long id, RedirectAttributes ra) {
        stateRepo.deleteById(id);
        ra.addFlashAttribute("success", "State deleted.");
        return "redirect:/admin/states";
    }

    // ─── Cities ──────────────────────────────────────────────────────────────
    @GetMapping("/cities")
    public String cities(Model model) {
        model.addAttribute("cities", cityRepo.findAll());
        model.addAttribute("states", stateRepo.findAll());
        model.addAttribute("city", new City());
        return "admin/cities";
    }

    @GetMapping("/cities/edit/{id}")
    public String editCity(@PathVariable Long id, Model model) {
        model.addAttribute("cities", cityRepo.findAll());
        model.addAttribute("states", stateRepo.findAll());
        model.addAttribute("city", cityRepo.findById(id).orElse(new City()));
        return "admin/cities";
    }

    @PostMapping("/cities/save")
    public String saveCity(@RequestParam(required = false) Long id,
                           @RequestParam String name,
                           @RequestParam(required = false) Long stateId,
                           RedirectAttributes ra) {
        City city = id != null ? cityRepo.findById(id).orElse(new City()) : new City();
        city.setName(name);
        city.setState(stateId != null ? stateRepo.findById(stateId).orElse(null) : null);
        cityRepo.save(city);
        ra.addFlashAttribute("success", "City saved.");
        return "redirect:/admin/cities";
    }

    @GetMapping("/cities/delete/{id}")
    public String deleteCity(@PathVariable Long id, RedirectAttributes ra) {
        cityRepo.deleteById(id);
        ra.addFlashAttribute("success", "City deleted.");
        return "redirect:/admin/cities";
    }

    // ─── E-Waste Master ──────────────────────────────────────────────────────
    @GetMapping("/ewaste-master")
    public String ewasteMaster(@RequestParam(required = false) Long categoryFilter, Model model) {
        var allEwastes = ewasteRepo.findAll();
        if (categoryFilter != null) {
            allEwastes = allEwastes.stream()
                .filter(e -> e.getCategory() != null && e.getCategory().getId().equals(categoryFilter))
                .toList();
        }
        model.addAttribute("ewastes", allEwastes);
        model.addAttribute("categories", categoryRepo.findAll());
        model.addAttribute("ewaste", new Ewaste());
        model.addAttribute("categoryFilter", categoryFilter);
        return "admin/ewastemaster";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@RequestParam String name,
                               @RequestParam(required = false) String description,
                               RedirectAttributes ra) {
        Category cat = new Category();
        cat.setName(name); cat.setDescription(description);
        categoryRepo.save(cat);
        ra.addFlashAttribute("success", "Category \"" + name + "\" added.");
        return "redirect:/admin/ewaste-master";
    }

    @GetMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes ra) {
        categoryRepo.deleteById(id);
        ra.addFlashAttribute("success", "Category deleted.");
        return "redirect:/admin/ewaste-master";
    }

    @GetMapping("/ewaste-master/edit/{id}")
    public String editEwaste(@PathVariable Long id, Model model, RedirectAttributes ra) {
        var item = ewasteRepo.findById(id);
        if (item.isEmpty()) { ra.addFlashAttribute("error", "Item not found."); return "redirect:/admin/ewaste-master"; }
        model.addAttribute("ewastes", ewasteRepo.findAll());
        model.addAttribute("categories", categoryRepo.findAll());
        model.addAttribute("ewaste", item.get());
        return "admin/ewastemaster";
    }

    @PostMapping("/ewaste-master/save")
    public String saveEwaste(@RequestParam(required = false) Long id,
                             @RequestParam String name,
                             @RequestParam(required = false) String description,
                             @RequestParam(required = false) Double pricePerKg,
                             @RequestParam(required = false) Long categoryId,
                             @RequestParam("imageFile") MultipartFile file,
                             RedirectAttributes ra) {
        try {
            Ewaste ewaste = id != null
                ? ewasteRepo.findById(id).orElse(new Ewaste())
                : new Ewaste();
            ewaste.setName(name);
            ewaste.setDescription(description);
            ewaste.setPricePerKg(pricePerKg);
            // Resolve category from DB (avoid detached entity)
            ewaste.setCategory(categoryId != null
                ? categoryRepo.findById(categoryId).orElse(null) : null);
            // Handle image
            if (!file.isEmpty()) {
                ewaste.setImagePath(commonFuns.saveFile(file));
            }
            // Keep existing image path if no new file and editing
            ewasteRepo.save(ewaste);
            ra.addFlashAttribute("success", id != null ? "Item updated." : "Item added.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Save failed: " + e.getMessage());
        }
        return "redirect:/admin/ewaste-master";
    }

    @GetMapping("/ewaste-master/remove-image/{id}")
    public String removeEwasteImage(@PathVariable Long id, RedirectAttributes ra) {
        ewasteRepo.findById(id).ifPresent(e -> {
            commonFuns.deleteFile(e.getImagePath());
            e.setImagePath(null);
            ewasteRepo.save(e);
        });
        ra.addFlashAttribute("success", "Image removed.");
        return "redirect:/admin/ewaste-master";
    }

    @GetMapping("/ewaste-master/delete/{id}")
    public String deleteEwaste(@PathVariable Long id, RedirectAttributes ra) {
        ewasteRepo.findById(id).ifPresent(e -> { commonFuns.deleteFile(e.getImagePath()); ewasteRepo.delete(e); });
        ra.addFlashAttribute("success", "Item deleted.");
        return "redirect:/admin/ewaste-master";
    }

    // ─── Registered Clients ──────────────────────────────────────────────────
    @GetMapping("/clients")
    public String registeredClients(Model model) {
        model.addAttribute("clients", clientRepo.findAll());
        return "admin/registeredclients";
    }

    @GetMapping("/clients/toggle/{id}")
    public String toggleClient(@PathVariable Long id, RedirectAttributes ra) {
        clientRepo.findById(id).ifPresent(c -> { c.setActive(!c.isActive()); clientRepo.save(c); });
        ra.addFlashAttribute("success", "Client status updated.");
        return "redirect:/admin/clients";
    }

    // ─── Registered Agents ───────────────────────────────────────────────────
    @GetMapping("/agents")
    public String registeredAgents(Model model) {
        var agents = agentRepo.findAll();
        long pendingCount = agents.stream().filter(a -> !a.isActive()).count();
        model.addAttribute("agents", agents);
        model.addAttribute("pendingAgentCount", pendingCount);
        return "admin/registeredagents";
    }

    @GetMapping("/agents/approve/{id}")
    public String approveAgent(@PathVariable Long id, RedirectAttributes ra) {
        agentRepo.findById(id).ifPresent(a -> {
            a.setActive(true);
            agentRepo.save(a);
            notifService.notifyAgent(a.getId(),
                "Your E-Waste Recycling Manager agent account has been approved! You can now log in and receive pickup assignments.");
        });
        ra.addFlashAttribute("success", "Agent approved.");
        return "redirect:/admin/agents";
    }

    @GetMapping("/agents/revoke/{id}")
    public String revokeAgent(@PathVariable Long id, RedirectAttributes ra) {
        agentRepo.findById(id).ifPresent(a -> { a.setActive(false); agentRepo.save(a); });
        ra.addFlashAttribute("success", "Agent access revoked.");
        return "redirect:/admin/agents";
    }

    @GetMapping("/agents/verify-license/{id}")
    public String verifyLicense(@PathVariable Long id, RedirectAttributes ra) {
        agentRepo.findById(id).ifPresent(a -> {
            a.setLicenseVerified(true);
            agentRepo.save(a);
            notifService.notifyAgent(a.getId(), "Your collector license has been verified by admin.");
        });
        ra.addFlashAttribute("success", "License verified.");
        return "redirect:/admin/agents";
    }

    @GetMapping("/agents/delete/{id}")
    public String deleteAgent(@PathVariable Long id, RedirectAttributes ra) {
        agentRepo.deleteById(id);
        ra.addFlashAttribute("success", "Agent removed.");
        return "redirect:/admin/agents";
    }

    // ─── Pickup Requests ─────────────────────────────────────────────────────
    @GetMapping("/pickups")
    public String allPickups(@RequestParam(required = false) String statusFilter,
                             @RequestParam(required = false) Long cityFilter,
                             Model model) {
        var allPickups = clientEwasteRepo.findAll();
        if (statusFilter != null && !statusFilter.isBlank()) {
            allPickups = allPickups.stream()
                .filter(p -> statusFilter.equalsIgnoreCase(p.getStatus())).toList();
        }
        // Build FULL tracking map: pickupId -> ALL assignments (INVITED + OFFERED + ACTIVE + REJECTED)
        var assignmentsMap = new java.util.HashMap<Long, java.util.List<com.project.ewastecs.entity.Clientewasteagent>>();
        var offersMap      = new java.util.HashMap<Long, java.util.List<com.project.ewastecs.entity.Clientewasteagent>>();
        var invitedMap     = new java.util.HashMap<Long, java.util.List<com.project.ewastecs.entity.Clientewasteagent>>();
        for (var pickup : allPickups) {
            var allAssign = assignmentRepo.findAllByClientEwasteId(pickup.getId());
            // Active (accepted) agents
            var active  = allAssign.stream().filter(a -> "ACTIVE".equals(a.getOfferStatus())).toList();
            // Agents who submitted offers (awaiting client decision)
            var offered = allAssign.stream().filter(a -> "OFFERED".equals(a.getOfferStatus())).toList();
            // Agents invited but haven't offered yet
            var invited = allAssign.stream().filter(a -> "INVITED".equals(a.getOfferStatus())).toList();
            if (!active.isEmpty())  assignmentsMap.put(pickup.getId(), active);
            if (!offered.isEmpty()) offersMap.put(pickup.getId(), offered);
            if (!invited.isEmpty()) invitedMap.put(pickup.getId(), invited);
        }
        // Agents filtered by city if cityFilter provided
        var allAgents = agentRepo.findByActiveTrue();
        var filteredAgents = (cityFilter != null)
            ? allAgents.stream().filter(a -> a.getCity() != null && a.getCity().getId().equals(cityFilter)).toList()
            : allAgents;

        // Build lightweight JSON maps for frontend modal rendering
        // Format: { "pickupId": [{ agentName, agentCity, offeredPrice, freeCollection, remarks }, ...] }
        StringBuilder offersJson   = new StringBuilder("{");
        StringBuilder invitedJson  = new StringBuilder("{");
        boolean firstO = true, firstI = true;
        for (var entry : offersMap.entrySet()) {
            if (!firstO) offersJson.append(",");
            offersJson.append("\"").append(entry.getKey()).append("\":[");
            boolean first2 = true;
            for (var a : entry.getValue()) {
			if (!first2) offersJson.append(",");
                String city    = a.getAgent().getCity() != null ? a.getAgent().getCity().getName() : "";
                String rawName = a.getAgent().getName();
                String rawRmk  = a.getRemarks() != null ? a.getRemarks() : "";
                // sanitize: remove chars that would break JSON string
                String safeName = rawName.replace("\\", "").replace("\"", "'").replace("\n", " ");
                String safeRmk  = rawRmk.replace("\\", "").replace("\"", "'").replace("\n", " ");
                offersJson.append("{")
                    .append("\"agentName\":\"").append(safeName).append("\",")
                    .append("\"agentCity\":\"").append(city).append("\",")
                    .append("\"offeredPrice\":").append(a.getOfferedPrice() != null ? a.getOfferedPrice() : "null").append(",")
                    .append("\"freeCollection\":").append(a.isFreeCollection()).append(",")
                    .append("\"remarks\":\"").append(safeRmk).append("\"")
                    .append("}");
                first2 = false;
            }
            offersJson.append("]");
            firstO = false;
        }
        offersJson.append("}");

        for (var entry : invitedMap.entrySet()) {
            if (!firstI) invitedJson.append(",");
            invitedJson.append("\"").append(entry.getKey()).append("\":[");
            boolean first2 = true;
            for (var a : entry.getValue()) {
			if (!first2) invitedJson.append(",");
                String agName = a.getAgent().getName().replace("\\", "").replace("\"", "'");
                invitedJson.append("\"").append(agName).append("\"");
                first2 = false;
            }
            invitedJson.append("]");
            firstI = false;
        }
        invitedJson.append("}");

        model.addAttribute("pickups", allPickups);
        model.addAttribute("agents", filteredAgents);
        model.addAttribute("allAgents", allAgents);
        model.addAttribute("cities", cityRepo.findAll());
        model.addAttribute("cityFilter", cityFilter);
        model.addAttribute("assignmentsMap", assignmentsMap);
        model.addAttribute("offersMap", offersMap);
        model.addAttribute("invitedMap", invitedMap);
        model.addAttribute("offersMapJson",  offersJson.toString());
        model.addAttribute("invitedMapJson", invitedJson.toString());
        model.addAttribute("statusFilter", statusFilter);
        return "admin/pickups";
    }

    // ─── Client Profile View ──────────────────────────────────────────────────
    @GetMapping("/clients/profile/{id}")
    public String clientProfile(@PathVariable Long id, Model model, RedirectAttributes ra) {
        var client = clientRepo.findById(id);
        if (client.isEmpty()) { ra.addFlashAttribute("error", "Client not found."); return "redirect:/admin/clients"; }
        var requests = clientEwasteRepo.findByClientId(id);
        long completed = requests.stream().filter(r -> "COMPLETED".equals(r.getStatus())).count();
        long pending   = requests.stream().filter(r -> "PENDING".equals(r.getStatus())).count();
        model.addAttribute("profileClient", client.get());
        model.addAttribute("requests", requests);
        model.addAttribute("completedCount", completed);
        model.addAttribute("pendingCount", pending);
        return "admin/clientprofile";
    }

    // ─── Agent Profile View ───────────────────────────────────────────────────
    @GetMapping("/agents/profile/{id}")
    public String agentProfile(@PathVariable Long id, Model model, RedirectAttributes ra) {
        var agent = agentRepo.findById(id);
        if (agent.isEmpty()) { ra.addFlashAttribute("error", "Agent not found."); return "redirect:/admin/agents"; }
        var assignments = assignmentRepo.findByAgentId(id);
        long collected = assignments.stream().filter(a -> "COMPLETED".equals(a.getClientEwaste().getStatus())).count();
        long pending   = assignments.stream().filter(a -> "ASSIGNED".equals(a.getClientEwaste().getStatus())).count();
        model.addAttribute("profileAgent", agent.get());
        model.addAttribute("assignments", assignments);
        model.addAttribute("collectedCount", collected);
        model.addAttribute("pendingCount", pending);
        return "admin/agentprofile";
    }

    /**
     * Admin invites multiple agents (by locality) to make price offers on a pickup.
     * Pickup status stays PENDING until the client accepts one of the offers.
     * Each invited agent gets a notification and sees the pickup in their "Invited" section.
     */
    @PostMapping("/pickups/assign")
    public String inviteAgents(@RequestParam Long pickupId,
                               @RequestParam(value = "agentIds", required = false) List<Long> agentIds,
                               RedirectAttributes ra) {
        if (agentIds == null || agentIds.isEmpty()) {
            ra.addFlashAttribute("error", "Please select at least one agent to invite.");
            return "redirect:/admin/pickups";
        }
        clientEwasteRepo.findById(pickupId).ifPresent(p -> {
            int invited = 0;
            for (Long agentId : agentIds) {
                var agentOpt = agentRepo.findById(agentId);
                if (agentOpt.isEmpty()) continue;
                var a = agentOpt.get();
                // Skip if agent already has any record (invited/offered/active) for this pickup
                boolean alreadyExists = assignmentRepo.existsByClientEwasteIdAndAgentId(pickupId, agentId);
                if (!alreadyExists) {
                    Clientewasteagent invite = new Clientewasteagent();
                    invite.setClientEwaste(p);
                    invite.setAgent(a);
                    invite.setAssignedAt(LocalDateTime.now());
                    invite.setOfferStatus("INVITED");
                    assignmentRepo.save(invite);
                    // Notify agent they have been invited to make an offer
                    notifService.notifyAgent(a.getId(),
                        "📦 You have been invited to make a price offer for: " +
                        p.getEwaste().getName() + " (" + p.getQuantity() + " unit(s)) — " +
                        p.getPickupAddress() + ". Go to Browse Requests to submit your offer.");
                    notifService.notifyAdmin("Invited agent " + a.getName() + " for pickup #" + pickupId);
                    invited++;
                }
            }
            final int invitedCount = invited;
            if (invitedCount > 0) {
                notifService.notifyClient(p.getClient().getId(),
                    "Admin has invited " + invitedCount + " local agent(s) to make price offers on your ["
                    + p.getEwaste().getName() + "]. You will be notified when offers arrive.");
            }
        });
        ra.addFlashAttribute("success", "Agent(s) invited to make price offers. They will be notified.");
        return "redirect:/admin/pickups";
    }

    @GetMapping("/pickups/cancel/{id}")
    public String cancelPickup(@PathVariable Long id, RedirectAttributes ra) {
        clientEwasteRepo.findById(id).ifPresent(p -> {
            p.setStatus("CANCELLED");
            clientEwasteRepo.save(p);
            notifService.notifyClient(p.getClient().getId(),
                "Your pickup request for \"" + p.getEwaste().getName() + "\" has been cancelled by admin.");
        });
        ra.addFlashAttribute("success", "Pickup cancelled.");
        return "redirect:/admin/pickups";
    }

    // ─── Contact Messages ────────────────────────────────────────────────────
    @GetMapping("/contacts")
    public String contacts(Model model) {
        model.addAttribute("contacts", contactRepo.findAll());
        return "admin/contactedlist";
    }

    @GetMapping("/contacts/reply/{id}")
    public String replyForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        var contact = contactRepo.findById(id);
        if (contact.isEmpty()) { ra.addFlashAttribute("error", "Not found."); return "redirect:/admin/contacts"; }
        var c = contact.get();
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        String defaultBody = """
            Dear %s,

            Thank you for reaching out to E-Waste Recycling Manager.

            We have received your message regarding "%s" and are happy to assist you.

            [Write your response here]

            We appreciate your interest in responsible e-waste recycling.

            Warm regards,
            E-Waste Recycling Manager Support Team
            Date: %s
            """.formatted(c.getName(), c.getSubject(), today);

        model.addAttribute("contact", c);
        model.addAttribute("defaultBody", defaultBody);
        model.addAttribute("today", today);
        return "admin/contactreply";
    }

    @PostMapping("/contacts/reply/{id}")
    public String sendReply(@PathVariable Long id,
                            @RequestParam String replySubject,
                            @RequestParam String replyBody,
                            RedirectAttributes ra) {
        contactRepo.findById(id).ifPresent(c -> {
            String htmlBody = "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;'>" +
                "<div style='background:#0c1116;padding:24px;border-radius:8px 8px 0 0;'>" +
                "<h2 style='color:#2dd4bf;margin:0;'>E-Waste Recycling Manager</h2></div>" +
                "<div style='background:#f8f8f8;padding:32px;border:1px solid #e0e0e0;'>" +
                "<pre style='font-family:Arial,sans-serif;white-space:pre-wrap;line-height:1.7;color:#333;'>" +
                replyBody + "</pre></div>" +
                "<div style='background:#0c1116;padding:16px;border-radius:0 0 8px 8px;text-align:center;" +
                "color:#6d8ea8;font-size:12px;'>E-Waste Recycling Manager — Responsible E-Waste Collection</div></div>";

            emailService.sendContactReply(c.getEmail(), c.getName(), replySubject, htmlBody);
            c.setReplied(true);
            contactRepo.save(c);
            ra.addFlashAttribute("success", "Reply sent to " + c.getEmail());
        });
        return "redirect:/admin/contacts";
    }

    @GetMapping("/contacts/mark-replied/{id}")
    public String markReplied(@PathVariable Long id, RedirectAttributes ra) {
        contactRepo.findById(id).ifPresent(c -> { c.setReplied(true); contactRepo.save(c); });
        ra.addFlashAttribute("success", "Marked as replied.");
        return "redirect:/admin/contacts";
    }

    // ─── Admin Notifications ─────────────────────────────────────────────────
    @PostMapping("/notifications/clear")
    public String clearAdminNotifications(RedirectAttributes ra) {
        notifService.markAllAdminRead();
        ra.addFlashAttribute("success", "Notifications cleared.");
        return "redirect:/admin/dashboard";
    }

    // ─── Change Password ─────────────────────────────────────────────────────
    @GetMapping("/change-password")
    public String changePasswordPage() { return "admin/changepassword"; }

    @PostMapping("/change-password")
    public String changePassword(Principal principal,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 RedirectAttributes ra) {
        adminRepo.findByEmail(principal.getName()).ifPresent(admin -> {
            if (encoder.matches(currentPassword, admin.getPassword())) {
                admin.setPassword(encoder.encode(newPassword));
                adminRepo.save(admin);
                ra.addFlashAttribute("success", "Password changed.");
            } else {
                ra.addFlashAttribute("error", "Current password incorrect.");
            }
        });
        return "redirect:/admin/change-password";
    }
}
