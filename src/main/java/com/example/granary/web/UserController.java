package com.example.granary.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.granary.business.RecipeService;
import com.example.granary.business.UserService;
import com.example.granary.dto.RecipeResponseDto;
import com.example.granary.dto.UserProfileDto;

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
}
