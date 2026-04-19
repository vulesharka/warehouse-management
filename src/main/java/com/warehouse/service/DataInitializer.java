package com.warehouse.service;

import com.warehouse.entity.User;
import com.warehouse.enums.Role;
import com.warehouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-username}")
    private String adminUsername;

    @Value("${app.seed.admin-email}")
    private String adminEmail;

    @Value("${app.seed.admin-password}")
    private String adminPassword;

    @Value("${app.seed.manager-username}")
    private String managerUsername;

    @Value("${app.seed.manager-email}")
    private String managerEmail;

    @Value("${app.seed.manager-password}")
    private String managerPassword;

    @Value("${app.seed.client-username}")
    private String clientUsername;

    @Value("${app.seed.client-email}")
    private String clientEmail;

    @Value("${app.seed.client-password}")
    private String clientPassword;

    @Override
    public void run(String... args) {
        seedUser(adminUsername,   adminEmail,   adminPassword,   Role.SYSTEM_ADMIN);
        seedUser(managerUsername, managerEmail, managerPassword, Role.WAREHOUSE_MANAGER);
        seedUser(clientUsername,  clientEmail,  clientPassword,  Role.CLIENT);
    }

    private void seedUser(String username, String email, String password, Role role) {
        if (!userRepository.existsByUsername(username)) {
            userRepository.save(User.builder()
                    .username(username)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .role(role)
                    .build());
            log.info("Seeded user '{}' with role {}", username, role);
        }
    }
}
