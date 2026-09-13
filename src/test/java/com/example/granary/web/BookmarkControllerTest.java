package com.example.granary.web;

import com.example.granary.business.BookmarkService;
import com.example.granary.business.UserService;
import com.example.granary.dto.RecipeResponseDto;
import com.example.granary.exceptions.NotLoggedInException;

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
 * Slice test for BookmarkController. It's a single, simple endpoint, so this
 * is intentionally short: the real behavior (idempotent add/remove, ownership
 * scoping) lives in BookmarkServiceTest, which already covers it well.
 *
 * Same Spring Boot 4 / security bypass notes as the other controller slice
 * tests apply here. Not run against a live build in my environment.
 */
@WebMvcTest(BookmarkController.class)
@AutoConfigureMockMvc(addFilters = false)
class BookmarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookmarkService bookmarkService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private UserService userService;

    @Test
    void getMyBookmarks_returnsOkWithList() throws Exception {
        RecipeResponseDto bookmarked = new RecipeResponseDto();
        bookmarked.setId(10L);
        when(bookmarkService.getMyBookmarks()).thenReturn(List.of(bookmarked));

        mockMvc.perform(get("/api/bookmarks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    void getMyBookmarks_empty_returnsOkWithEmptyList() throws Exception {
        when(bookmarkService.getMyBookmarks()).thenReturn(List.of());

        mockMvc.perform(get("/api/bookmarks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getMyBookmarks_notLoggedIn_returns401() throws Exception {
        // Real enforcement of "GET /api/bookmarks requires auth" lives in
        // SecurityConfig (bypassed here by addFilters = false); this only
        // covers what GlobalExceptionHandler does if the service is reached
        // without a logged-in user anyway.
        when(bookmarkService.getMyBookmarks())
                .thenThrow(new NotLoggedInException("You must be logged in to perform this action"));

        mockMvc.perform(get("/api/bookmarks"))
                .andExpect(status().isUnauthorized());
    }
}
