package com.example.granary.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import com.example.granary.dto.UserProfileDto;
import com.example.granary.exceptions.UserNotFoundException;
import com.example.granary.model.Role;
import com.example.granary.model.User;
import com.example.granary.repo.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private ImageStorageService imageStorageService;

    private UserService userService;

    private User owner;
    private User admin;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, currentUserService, imageStorageService);

        owner = user(1L, "alice");
        admin = user(2L, "root");
        admin.setRole(Role.ADMIN);
    }

    private static User user(Long id, String username) {
        User u = new User(username, username + "@test.com", "hash");
        u.setId(id);
        return u;
    }

    private static void save(UserRepository repo) {
        when(repo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // -------------------------
    // getProfile
    // -------------------------

    @Test
    @DisplayName("getProfile - returns username, description, and avatar url")
    void getProfile_found_returnsProfile() {
        owner.setDescription("Home cook.");
        owner.setAvatarFilename("pic.png");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(owner));

        UserProfileDto profile = userService.getProfile("alice");

        assertThat(profile.getUsername()).isEqualTo("alice");
        assertThat(profile.getDescription()).isEqualTo("Home cook.");
        assertThat(profile.getAvatarUrl()).isEqualTo("/images/pic.png");
    }

    @Test
    @DisplayName("getProfile - avatar url is null when no avatar is set")
    void getProfile_noAvatar_nullUrl() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(owner));

        UserProfileDto profile = userService.getProfile("alice");

        assertThat(profile.getAvatarUrl()).isNull();
    }

    @Test
    @DisplayName("getProfile - throws when the user does not exist")
    void getProfile_notFound_throwsUserNotFoundException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile("ghost"))
                .isInstanceOf(UserNotFoundException.class);
    }

    // -------------------------
    // updateDescription
    // -------------------------

    @Test
    @DisplayName("updateDescription - owner can edit their own description")
    void updateDescription_ownerSuccess() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(owner));
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        save(userRepository);

        UserProfileDto profile = userService.updateDescription("alice", "New bio");

        assertThat(profile.getDescription()).isEqualTo("New bio");
    }

    @Test
    @DisplayName("updateDescription - admin can edit another user's description")
    void updateDescription_adminSuccess() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(owner));
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        save(userRepository);

        UserProfileDto profile = userService.updateDescription("alice", "Moderated bio");

        assertThat(profile.getDescription()).isEqualTo("Moderated bio");
    }

    @Test
    @DisplayName("updateDescription - throws when a different, non-admin user tries to edit")
    void updateDescription_notOwner_throwsAccessDenied() {
        User other = user(3L, "eve");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(owner));
        when(currentUserService.getCurrentUser()).thenReturn(other);

        assertThatThrownBy(() -> userService.updateDescription("alice", "hacked"))
                .isInstanceOf(AccessDeniedException.class);

        verify(userRepository, never()).save(any());
    }

    // -------------------------
    // uploadAvatar
    // -------------------------

    @Test
    @DisplayName("uploadAvatar - owner can upload their own avatar")
    void uploadAvatar_ownerSuccess() {
        MultipartFile file = new MockMultipartFile("file", "me.png", "image/png", new byte[] { 1, 2, 3 });
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(owner));
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(imageStorageService.store(file)).thenReturn("generated.png");
        save(userRepository);

        UserProfileDto profile = userService.uploadAvatar("alice", file);

        assertThat(profile.getAvatarUrl()).isEqualTo("/images/generated.png");
    }

    @Test
    @DisplayName("uploadAvatar - replacing an avatar deletes the old file")
    void uploadAvatar_replacesExisting_deletesOldFile() {
        owner.setAvatarFilename("old.png");
        MultipartFile file = new MockMultipartFile("file", "me.png", "image/png", new byte[] { 1, 2, 3 });
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(owner));
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(imageStorageService.store(file)).thenReturn("generated.png");
        save(userRepository);

        userService.uploadAvatar("alice", file);

        verify(imageStorageService).delete("old.png");
    }

    @Test
    @DisplayName("uploadAvatar - throws when a different, non-admin user tries to upload")
    void uploadAvatar_notOwner_throwsAccessDenied() {
        User other = user(3L, "eve");
        MultipartFile file = new MockMultipartFile("file", "me.png", "image/png", new byte[] { 1, 2, 3 });
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(owner));
        when(currentUserService.getCurrentUser()).thenReturn(other);

        assertThatThrownBy(() -> userService.uploadAvatar("alice", file))
                .isInstanceOf(AccessDeniedException.class);

        verify(imageStorageService, never()).store(any());
    }

    @Test
    @DisplayName("uploadAvatar - rejects an unsupported file type")
    void uploadAvatar_wrongType_throwsIllegalArgument() {
        MultipartFile file = new MockMultipartFile("file", "me.txt", "text/plain", new byte[] { 1 });
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(owner));
        when(currentUserService.getCurrentUser()).thenReturn(owner);

        assertThatThrownBy(() -> userService.uploadAvatar("alice", file))
                .isInstanceOf(IllegalArgumentException.class);

        verify(imageStorageService, never()).store(any());
    }

    @Test
    @DisplayName("uploadAvatar - rejects a file over the 2MB limit")
    void uploadAvatar_tooLarge_throwsIllegalArgument() {
        byte[] oversized = new byte[2 * 1024 * 1024 + 1];
        MultipartFile file = new MockMultipartFile("file", "me.png", "image/png", oversized);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(owner));
        when(currentUserService.getCurrentUser()).thenReturn(owner);

        assertThatThrownBy(() -> userService.uploadAvatar("alice", file))
                .isInstanceOf(IllegalArgumentException.class);

        verify(imageStorageService, never()).store(any());
    }

    // -------------------------
    // deleteAvatar
    // -------------------------

    @Test
    @DisplayName("deleteAvatar - owner can remove their own avatar")
    void deleteAvatar_ownerSuccess() {
        owner.setAvatarFilename("me.png");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(owner));
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        save(userRepository);

        UserProfileDto profile = userService.deleteAvatar("alice");

        assertThat(profile.getAvatarUrl()).isNull();
        verify(imageStorageService).delete("me.png");
    }

    @Test
    @DisplayName("deleteAvatar - admin can remove another user's avatar")
    void deleteAvatar_adminSuccess() {
        owner.setAvatarFilename("me.png");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(owner));
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        save(userRepository);

        userService.deleteAvatar("alice");

        verify(imageStorageService).delete("me.png");
    }

    @Test
    @DisplayName("deleteAvatar - throws when a different, non-admin user tries to remove it")
    void deleteAvatar_notOwner_throwsAccessDenied() {
        owner.setAvatarFilename("me.png");
        User other = user(3L, "eve");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(owner));
        when(currentUserService.getCurrentUser()).thenReturn(other);

        assertThatThrownBy(() -> userService.deleteAvatar("alice"))
                .isInstanceOf(AccessDeniedException.class);

        verify(imageStorageService, never()).delete(any());
    }
}
