package com.example.granary.business;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.granary.exceptions.NotLoggedInException;
import com.example.granary.model.User;
import com.example.granary.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    @Value("${app.dev.fallback-username:}")
    private String devFallbackUsername;

    public User getCurrentUser() throws NotLoggedInException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean noRealUser = authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser");

        if (noRealUser) {
            if (!devFallbackUsername.isBlank()) {
                return userRepository.findByUsername(devFallbackUsername)
                        .orElseThrow(() -> new NotLoggedInException(
                                "Dev fallback user not found: " + devFallbackUsername));
            }
            throw new NotLoggedInException("You must be logged in to perform this action");
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Logged in user not found: " + username
                ));
    }

    public boolean isOwner(User recipeOwner) throws NotLoggedInException {
        return recipeOwner.getId().equals(getCurrentUser().getId());
    }
}