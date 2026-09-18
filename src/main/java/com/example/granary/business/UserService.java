package com.example.granary.business;

import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.granary.dto.UserProfileDto;
import com.example.granary.exceptions.UserNotFoundException;
import com.example.granary.model.User;
import com.example.granary.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private static final long MAX_AVATAR_SIZE_BYTES = 2L * 1024 * 1024; // 2MB
    private static final Map<String, String> ALLOWED_AVATAR_TYPES = Map.of(
        "image/jpeg", "jpg",
        "image/png", "png",
        "image/webp", "webp",
        "image/gif", "gif"
    );

    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ImageStorageService imageStorageService;

    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    public UserProfileDto getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
        return toProfileDto(user);
    }

    public UserProfileDto updateDescription(String username, String description) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        assertCanModify(user, currentUserService.getCurrentUser());

        user.setDescription(description);
        return toProfileDto(userRepository.save(user));
    }

    public UserProfileDto uploadAvatar(String username, MultipartFile file) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        assertCanModify(user, currentUserService.getCurrentUser());
        validateAvatarFile(file);

        if (user.getAvatarFilename() != null) {
            imageStorageService.delete(user.getAvatarFilename());
        }

        String filename = imageStorageService.store(file);
        user.setAvatarFilename(filename);
        return toProfileDto(userRepository.save(user));
    }

    public UserProfileDto deleteAvatar(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        assertCanModify(user, currentUserService.getCurrentUser());

        if (user.getAvatarFilename() != null) {
            imageStorageService.delete(user.getAvatarFilename());
            user.setAvatarFilename(null);
        }
        return toProfileDto(userRepository.save(user));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    private UserProfileDto toProfileDto(User user) {
        return UserProfileDto.builder()
                .username(user.getUsername())
                .description(user.getDescription())
                .avatarUrl(user.getAvatarFilename() == null ? null : "/images/" + user.getAvatarFilename())
                .build();
    }

    // A user may modify their own profile; admins may modify anyone's.
    private void assertCanModify(User targetUser, User currentUser) {
        if (currentUser.isAdmin()) {
            return;
        }
        if (!targetUser.getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You do not have permission to modify this profile");
        }
    }

    private void validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Avatar file cannot be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_AVATAR_TYPES.containsKey(contentType)) {
            throw new IllegalArgumentException(
                "Invalid file type. Allowed types: JPEG, PNG, WEBP, GIF"
            );
        }

        if (file.getSize() > MAX_AVATAR_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds the 2MB limit");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("File must have a valid name");
        }

        String extension = originalFilename.substring(
            originalFilename.lastIndexOf(".") + 1
        ).toLowerCase();

        boolean extensionValid = extension.equals(ALLOWED_AVATAR_TYPES.get(contentType))
                || (contentType.equals("image/jpeg") && extension.equals("jpeg"));

        if (!extensionValid) {
            throw new IllegalArgumentException("File extension does not match its content type");
        }
    }
}
