package com.project.ewastecs;

import com.project.ewastecs.entity.Admin;
import com.project.ewastecs.repository.AdminRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Runs once on startup. Creates a default admin account if none exists.
 * Default credentials:  admin@ewastecs.in  /  admin@123
 * CHANGE THE PASSWORD after first login via /admin/change-password
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final AdminRepository adminRepo;
    private final PasswordEncoder encoder;

    public DataInitializer(AdminRepository adminRepo, PasswordEncoder encoder) {
        this.adminRepo = adminRepo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (adminRepo.count() == 0) {
            Admin admin = new Admin();
            admin.setEmail("admin@ewastecs.in");
            admin.setPassword(encoder.encode("admin@123"));
            admin.setName("E-Waste Recycling Manager Admin");
            admin.setMobile("9000000000");
            adminRepo.save(admin);
            System.out.println("==============================================");
            System.out.println("  Default admin account created:");
            System.out.println("  Email   : admin@ewastecs.in");
            System.out.println("  Password: admin@123");
            System.out.println("  URL     : http://localhost:8080/admin/login");
            System.out.println("  CHANGE THE PASSWORD after first login!");
            System.out.println("==============================================");
        }
    }
}
