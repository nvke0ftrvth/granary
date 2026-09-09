package com.example.granary.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.granary.model.User;
import com.example.granary.repo.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Dev-only: ensures a "testuser" exists so app.dev.fallback-username
 * (CurrentUserService) has a real user to attach unauthenticated requests to.
 * Never active outside the "dev" profile.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        userRepository.findByUsername("testuser").ifPresentOrElse(
            user -> log.debug("Dev seed: testuser already exists"),
            () -> {
                User devUser = new User("testuser", "testuser@example.com",
                        passwordEncoder.encode("dev-password-not-for-prod"));
                userRepository.save(devUser);
                log.info("Dev seed: created testuser");
            }
        );
    }
}