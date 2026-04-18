package com.project.ewastecs.security;

import com.project.ewastecs.entity.Admin;
import com.project.ewastecs.entity.Agent;
import com.project.ewastecs.entity.Client;
import com.project.ewastecs.repository.AdminRepository;
import com.project.ewastecs.repository.AgentRepository;
import com.project.ewastecs.repository.ClientsRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserloginDetailsService implements UserDetailsService {

    private final AdminRepository adminRepo;
    private final ClientsRepository clientRepo;
    private final AgentRepository agentRepo;

    public UserloginDetailsService(AdminRepository adminRepo,
                                   ClientsRepository clientRepo,
                                   AgentRepository agentRepo) {
        this.adminRepo = adminRepo;
        this.clientRepo = clientRepo;
        this.agentRepo = agentRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // Check Admin
        Optional<Admin> admin = adminRepo.findByEmail(email);
        if (admin.isPresent()) {
            return new org.springframework.security.core.userdetails.User(
                admin.get().getEmail(),
                admin.get().getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );
        }

        // Check Client
        Optional<Client> client = clientRepo.findByEmail(email);
        if (client.isPresent() && client.get().isActive()) {
            return new org.springframework.security.core.userdetails.User(
                client.get().getEmail(),
                client.get().getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))
            );
        }

        // Check Agent
        Optional<Agent> agent = agentRepo.findByEmail(email);
        if (agent.isPresent() && agent.get().isActive()) {
            return new org.springframework.security.core.userdetails.User(
                agent.get().getEmail(),
                agent.get().getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_AGENT"))
            );
        }

        throw new UsernameNotFoundException("No active user found with email: " + email);
    }
}
