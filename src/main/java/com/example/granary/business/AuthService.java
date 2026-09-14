package com.example.granary.business;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.granary.dto.AuthResponseDto;
import com.example.granary.dto.LoginRequestDto;
import com.example.granary.dto.RegisterRequestDto;
import com.example.granary.model.User;
import com.example.granary.repo.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponseDto register(RegisterRequestDto dto) {
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username is already taken");
        }

        User user = new User(
                dto.getUsername(),
                dto.getEmail(),
                passwordEncoder.encode(dto.getPassword())
        );
        User saved = userRepository.save(user);
        log.info("Registered new user: {}", saved.getUsername());

        String token = jwtService.generateToken(saved);
        return new AuthResponseDto(token, saved.getUsername());
    }

    public AuthResponseDto login(LoginRequestDto dto) {
        // Throws BadCredentialsException (mapped to 401 in GlobalExceptionHandler)
        // if the username doesn't exist or the password doesn't match.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
        );

        User user = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(); // unreachable if authenticate() succeeded above

        String token = jwtService.generateToken(user);
        log.info("User logged in: {}", user.getUsername());
        return new AuthResponseDto(token, user.getUsername());
    }
}
