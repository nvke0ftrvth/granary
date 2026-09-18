package com.example.granary.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.granary.business.RecipeService;
import com.example.granary.business.UserService;
import com.example.granary.dto.RecipeResponseDto;
import com.example.granary.dto.UpdateProfileRequestDto;
import com.example.granary.dto.UserProfileDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RecipeService recipeService;

    @GetMapping("/{username}")
    public ResponseEntity<UserProfileDto> getProfile(@PathVariable String username) {
        return ResponseEntity.ok(userService.getProfile(username));
    }

    @GetMapping("/{username}/recipes")
    public ResponseEntity<List<RecipeResponseDto>> getRecipes(@PathVariable String username) {
        return ResponseEntity.ok(recipeService.getByUsername(username));
    }

    // PUT update a user's description (owner or admin -- enforced in UserService)
    @PutMapping("/{username}")
    public ResponseEntity<UserProfileDto> updateProfile(
            @PathVariable String username,
            @Valid @RequestBody UpdateProfileRequestDto dto) {
        return ResponseEntity.ok(userService.updateDescription(username, dto.getDescription()));
    }

    // POST upload/replace a user's profile picture (owner or admin -- enforced in UserService)
    @PostMapping("/{username}/avatar")
    public ResponseEntity<UserProfileDto> uploadAvatar(
            @PathVariable String username,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userService.uploadAvatar(username, file));
    }

    // DELETE remove a user's profile picture (owner or admin -- enforced in UserService)
    @DeleteMapping("/{username}/avatar")
    public ResponseEntity<UserProfileDto> deleteAvatar(@PathVariable String username) {
        return ResponseEntity.ok(userService.deleteAvatar(username));
    }
}
