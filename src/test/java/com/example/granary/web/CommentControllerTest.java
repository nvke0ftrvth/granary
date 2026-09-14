package com.example.granary.web;

import com.example.granary.business.CommentService;
import com.example.granary.business.UserService;
import com.example.granary.dto.CommentRequestDto;
import com.example.granary.dto.CommentResponseDto;
import com.example.granary.dto.CommentVoteRequestDto;
import com.example.granary.exceptions.ResourceNotFoundException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for CommentController: everything under its class-level
 * @RequestMapping("/api/comments") -- edit, soft-delete, and voting. The
 * recipe-scoped listing/creation routes (GET/POST /api/recipes/{id}/comments)
 * are NOT part of this controller -- they live on RecipeController -- so
 * those cases are covered by RecipeControllerWebMvcTest instead, not here.
 *
 * Same Spring Boot 4 / security bypass notes as RecipeControllerWebMvcTest
 * apply here: @MockitoBean (not @MockBean), @AutoConfigureMockMvc(addFilters
 * = false), and JwtAuthenticationFilter/UserService mocked out to satisfy
 * bean wiring without dragging in real security.
 */
@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(Jackson2AutoConfiguration.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private UserService userService;

    // ------------------------------------------------------------- happy path

    @Test
    void updateComment_returnsOk() throws Exception {
        CommentRequestDto request = new CommentRequestDto("Edited", null);
        when(commentService.update(eq(1L), any())).thenReturn(CommentResponseDto.builder().id(1L).content("Edited").build());

        mockMvc.perform(put("/api/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Edited"));
    }

    @Test
    void deleteComment_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/comments/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void voteComment_returnsOk() throws Exception {
        CommentVoteRequestDto request = new CommentVoteRequestDto(1);
        when(commentService.vote(1L, 1)).thenReturn(CommentResponseDto.builder().id(1L).score(1).currentUserVote(1).build());

        mockMvc.perform(put("/api/comments/1/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(1));
    }

    @Test
    void voteComment_missingValue_returns400() throws Exception {
        CommentVoteRequestDto request = new CommentVoteRequestDto(null); // fails @NotNull

        mockMvc.perform(put("/api/comments/1/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Vote value is required")));
    }

    @Test
    void removeVote_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/comments/1/vote"))
                .andExpect(status().isNoContent());
    }

    // -------------------------------------------------- GlobalExceptionHandler

    @Nested
    class ExceptionMapping {

        @Test
        void commentNotFound_onUpdate_returns404() throws Exception {
            when(commentService.update(eq(999L), any()))
                    .thenThrow(new ResourceNotFoundException("Comment", 999L));

            mockMvc.perform(put("/api/comments/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CommentRequestDto("hi", null))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(containsString("999")));
        }

        @Test
        void notOwner_onUpdate_returns403() throws Exception {
            when(commentService.update(eq(1L), any()))
                    .thenThrow(new AccessDeniedException("You do not have permission to modify this comment"));

            mockMvc.perform(put("/api/comments/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CommentRequestDto("hack", null))))
                    .andExpect(status().isForbidden());
        }

        @Test
        void notOwner_onDelete_returns403() throws Exception {
            org.mockito.Mockito.doThrow(new AccessDeniedException("You do not have permission to modify this comment"))
                    .when(commentService).delete(1L);

            mockMvc.perform(delete("/api/comments/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void editingDeletedComment_returns400() throws Exception {
            when(commentService.update(eq(1L), any()))
                    .thenThrow(new IllegalArgumentException("Cannot edit a deleted comment"));

            mockMvc.perform(put("/api/comments/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CommentRequestDto("hi", null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Cannot edit a deleted comment"));
        }

        @Test
        void votingOnDeletedComment_returns400() throws Exception {
            when(commentService.vote(eq(1L), any()))
                    .thenThrow(new IllegalArgumentException("Cannot vote on a deleted comment"));

            mockMvc.perform(put("/api/comments/1/vote")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CommentVoteRequestDto(1))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Cannot vote on a deleted comment"));
        }
    }
}
