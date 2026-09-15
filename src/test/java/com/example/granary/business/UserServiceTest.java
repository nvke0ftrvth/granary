package com.example.granary.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.granary.dto.UserProfileDto;
import com.example.granary.exceptions.UserNotFoundException;
import com.example.granary.model.User;
import com.example.granary.repo.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void getProfile_found_returnsUsername() {
        User user = new User("alice", "alice@test.com", "hash");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserProfileDto profile = userService.getProfile("alice");

        assertThat(profile.getUsername()).isEqualTo("alice");
    }

    @Test
    void getProfile_notFound_throwsUserNotFoundException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile("ghost"))
                .isInstanceOf(UserNotFoundException.class);
    }
}
