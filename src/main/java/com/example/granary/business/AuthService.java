package com.example.granary.business;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.granary.dto.AuthResponseDto;
import com.example.granary.dto.LoginRequestDto;
import com.example.granary.dto.RegisterRequestDto;
import com.example.granary.exceptions.UserNotFoundException;
import com.example.granary.model.User;
import com.example.granary.repo.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    public AuthResponseDto register(RegisterRequestDto request) {

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException(
                "Username '" + request.getUsername() + "' is already taken"
            );
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException(
                "Email '" + request.getEmail() + "' is already registered"
            );
        }

        // Build and save the new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // hash it

        User saved = userRepository.save(user);
        log.info("New user registered: {}", saved.getUsername());

        // Generate token and return response
        String token = jwtService.generateToken(saved);
        return buildAuthResponse(saved, token);
    }

    // -------------------------
    // Login
    // Returns: AuthResponseDto
    // -------------------------
    public AuthResponseDto login(LoginRequestDto request) {

        // This throws AuthenticationException if credentials are wrong
        // Spring Security handles the actual credential check
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            )
        );

        // If we get here credentials were valid — load the user
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UserNotFoundException(
                    "User not found: " + request.getUsername()
                ));

        log.info("User logged in: {}", user.getUsername());

        String token = jwtService.generateToken(user);
        return buildAuthResponse(user, token);
    }

    // -------------------------
    // Shared response builder
    // -------------------------
    private AuthResponseDto buildAuthResponse(User user, String token) {
        return AuthResponseDto.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .userId(user.getId())
                .expiresIn(jwtExpiration)
                .build();
    }
}