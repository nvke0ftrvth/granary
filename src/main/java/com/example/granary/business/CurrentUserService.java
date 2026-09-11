package com.example.granary.business;

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

    public User getCurrentUser() throws NotLoggedInException {
        String username = getAuthenticatedUsernameOrNull();
        if (username == null) {
            throw new NotLoggedInException("You must be logged in to perform this action");
        }

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Logged in user not found: " + username
                ));
    }

    public User getCurrentUserOrNull() {
        String username = getAuthenticatedUsernameOrNull();
        if (username == null) {
            return null;
        }
        return userRepository.findByUsername(username).orElse(null);
    }

    public boolean isOwner(User recipeOwner) throws NotLoggedInException {
        return recipeOwner.getId().equals(getCurrentUser().getId());
    }

    private String getAuthenticatedUsernameOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        return authentication.getName();
    }
}
