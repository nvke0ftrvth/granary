package com.example.granary.web;

import com.example.granary.business.RecipeService;
import com.example.granary.business.UserService;
import com.example.granary.dto.RecipeResponseDto;
import com.example.granary.dto.UserProfileDto;
import com.example.granary.exceptions.UserNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for UserController -- both routes are public (permitAll in
 * SecurityConfig, real enforcement not exercised here since addFilters =
 * false), so these tests focus on happy-path 200s and the 404 mapping for
 * an unknown username via GlobalExceptionHandler.
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RecipeService recipeService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getProfile_found_returnsOk() throws Exception {
        when(userService.getProfile("alice")).thenReturn(UserProfileDto.builder().username("alice").build());

        mockMvc.perform(get("/api/users/alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void getProfile_unknownUsername_returns404() throws Exception {
        when(userService.getProfile("ghost")).thenThrow(new UserNotFoundException("ghost"));

        mockMvc.perform(get("/api/users/ghost"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("ghost")));
    }

    @Test
    void getRecipes_returnsThatUsersRecipes() throws Exception {
        RecipeResponseDto recipe = new RecipeResponseDto();
        recipe.setId(1L);
        recipe.setOwnerUsername("alice");
        when(recipeService.getByUsername("alice")).thenReturn(List.of(recipe));

        mockMvc.perform(get("/api/users/alice/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ownerUsername").value("alice"));
    }

    @Test
    void getRecipes_noRecipes_returnsEmptyList() throws Exception {
        when(recipeService.getByUsername("alice")).thenReturn(List.of());

        mockMvc.perform(get("/api/users/alice/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
