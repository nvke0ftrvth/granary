package com.example.granary.web;

import com.example.granary.business.BookmarkService;
import com.example.granary.business.CommentService;
import com.example.granary.business.RecipeService;
import com.example.granary.business.UserService;
import com.example.granary.dto.CommentRequestDto;
import com.example.granary.dto.CommentResponseDto;
import com.example.granary.dto.RecipeRequestDto;
import com.example.granary.dto.RecipeResponseDto;
import com.example.granary.exceptions.NotLoggedInException;
import com.example.granary.exceptions.RecipeNotFoundException;
import com.example.granary.model.Ingredient;
import com.example.granary.model.Step;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for RecipeController + the real GlobalExceptionHandler, with
 * RecipeService/CommentService/BookmarkService all mocked. Deliberately NOT
 * named "RecipeControllerTest" -- that name is already taken by this
 * project's full-stack integration test (com.example.granary
 * .RecipeControllerTest, extends BaseIntegrationTest).
 *
 * RecipeController owns GET/POST "/{id}/comments" (full path
 * /api/recipes/{id}/comments) -- CommentController only handles
 * /api/comments/{id} (update/delete/vote), so there's no route collision.
 * The four validation/error-path tests for comment creation below
 * (createComment_blankContent_returns400, createComment_tooLong_returns400,
 * recipeNotFound_onCreate_returns404, parentFromDifferentRecipe_returns400)
 * were moved here from CommentControllerTest, which had mistakenly tested
 * these routes against @WebMvcTest(CommentController.class) -- a slice that
 * never registers a handler for them.
 *
 * Security note: RecipeService/CommentService/BookmarkService are mocked, so
 * none of SecurityConfig's real authorization logic runs here regardless.
 * @AutoConfigureMockMvc(addFilters = false) disables the servlet filter
 * chain for these requests to keep the slice fast and focused purely on
 * controller + exception-advice behavior. JwtAuthenticationFilter and
 * UserService are mocked out as beans because JwtAuthenticationFilter is a
 * @Component implementing Filter, which @WebMvcTest picks up regardless of
 * addFilters, and its real constructor needs a live JwtService/UserService.
 *
 * Built for Spring Boot 4 (@MockitoBean, not the removed @MockBean). I
 * couldn't run `mvn test` in my environment (no Maven Central access), so
 * please run this for real and send me anything that fails to compile or
 * load context.
 */
@WebMvcTest(RecipeController.class)
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(Jackson2AutoConfiguration.class)
class RecipeControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RecipeService recipeService;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private BookmarkService bookmarkService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private UserService userService;

    // happy path

    @Test
    void getAllRecipes_returnsOkWithList() throws Exception {
        when(recipeService.getAll()).thenReturn(List.of(new RecipeResponseDto()));

        mockMvc.perform(get("/api/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getMyRecipes_returnsOk() throws Exception {
        when(recipeService.getMine()).thenReturn(List.of(new RecipeResponseDto()));

        mockMvc.perform(get("/api/recipes/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getMyRecipes_notLoggedIn_returns401() throws Exception {
        when(recipeService.getMine()).thenThrow(new NotLoggedInException("You must be logged in to perform this action"));

        mockMvc.perform(get("/api/recipes/mine"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getRecipeById_found_returnsOk() throws Exception {
        RecipeResponseDto dto = new RecipeResponseDto();
        dto.setId(1L);
        when(recipeService.getById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/recipes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getByCategory_returnsMatches() throws Exception {
        when(recipeService.getByTag("breakfast")).thenReturn(List.of(new RecipeResponseDto()));

        mockMvc.perform(get("/api/recipes/tag/breakfast"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void search_withQueryParam_returnsOk() throws Exception {
        when(recipeService.search("pancake")).thenReturn(List.of(new RecipeResponseDto()));

        mockMvc.perform(get("/api/recipes/search").param("query", "pancake"))
                .andExpect(status().isOk());
    }

    @Test
    void getPopularRecipes_returnsOkWithList() throws Exception {
        RecipeResponseDto dto = new RecipeResponseDto();
        dto.setId(7L);
        when(bookmarkService.getMostBookmarkedRecipes(8)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/recipes/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(7));
    }

    @Test
    void createRecipe_valid_returns201WithLocationHeader() throws Exception {
        RecipeResponseDto response = new RecipeResponseDto();
        response.setId(42L);
        when(recipeService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequestDto())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/recipes/42")));
    }

    @Test
    void updateRecipe_valid_returnsOk() throws Exception {
        when(recipeService.update(eq(1L), any())).thenReturn(new RecipeResponseDto());

        mockMvc.perform(put("/api/recipes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequestDto())))
                .andExpect(status().isOk());
    }

    @Test
    void deleteRecipe_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/recipes/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void uploadImages_returnsOk() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "a.png", "image/png", new byte[]{1});
        when(recipeService.uploadImages(eq(1L), anyList())).thenReturn(new RecipeResponseDto());

        mockMvc.perform(multipart("/api/recipes/1/images").file(file))
                .andExpect(status().isOk());
    }

    @Test
    void deleteImage_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/recipes/1/images/9"))
                .andExpect(status().isNoContent());
    }

    @Test
    void reorderImages_returnsOk() throws Exception {
        when(recipeService.reorderImages(eq(1L), anyList())).thenReturn(new RecipeResponseDto());

        mockMvc.perform(put("/api/recipes/1/images/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(3, 1, 2))))
                .andExpect(status().isOk());
    }

    // comments/bookmarks via RecipeController

    @Test
    void getComments_returnsOk() throws Exception {
        when(commentService.getByRecipe(1L)).thenReturn(List.of(CommentResponseDto.builder().id(1L).build()));

        mockMvc.perform(get("/api/recipes/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void createComment_returns201() throws Exception {
        CommentRequestDto dto = new CommentRequestDto("Looks great!", null);
        when(commentService.create(eq(1L), any())).thenReturn(CommentResponseDto.builder().id(5L).build());

        mockMvc.perform(post("/api/recipes/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void createComment_blankContent_returns400() throws Exception {
        CommentRequestDto request = new CommentRequestDto("   ", null);

        mockMvc.perform(post("/api/recipes/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Comment cannot be empty")));
    }

    @Test
    void createComment_tooLong_returns400() throws Exception {
        CommentRequestDto request = new CommentRequestDto("x".repeat(2001), null); // fails @Size(max = 2000)

        mockMvc.perform(post("/api/recipes/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("2000 characters")));
    }

    @Test
    void bookmarkRecipe_returnsNoContent() throws Exception {
        mockMvc.perform(post("/api/recipes/1/bookmark"))
                .andExpect(status().isNoContent());
    }

    @Test
    void unbookmarkRecipe_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/recipes/1/bookmark"))
                .andExpect(status().isNoContent());
    }

    // GlobalExceptionHandler

    @Nested
    class ExceptionMapping {

        @Test
        void resourceNotFound_returns404WithApiErrorBody() throws Exception {
            when(recipeService.getById(99L)).thenThrow(new RecipeNotFoundException(99L));

            mockMvc.perform(get("/api/recipes/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Recipe with id 99 not found"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        void notLoggedIn_returns401() throws Exception {
            when(recipeService.create(any())).thenThrow(new NotLoggedInException("You must be logged in to perform this action"));

            mockMvc.perform(post("/api/recipes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequestDto())))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("You must be logged in to perform this action"));
        }

        @Test
        void notOwner_returns403() throws Exception {
            when(recipeService.update(eq(1L), any()))
                    .thenThrow(new AccessDeniedException("You do not have permission to modify this recipe"));

            mockMvc.perform(put("/api/recipes/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequestDto())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("You do not have permission to modify this recipe"));
        }

        @Test
        void springSecurityAuthenticationException_nowCorrectlyReturns401() throws Exception {

            when(recipeService.getById(1L))
                    .thenThrow(new InsufficientAuthenticationException("full authentication required"));

            mockMvc.perform(get("/api/recipes/1"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Authentication failed"));
        }

        @Test
        void beanValidationFailure_returns400WithFieldErrorMessages() throws Exception {
            RecipeRequestDto invalid = new RecipeRequestDto();

            mockMvc.perform(post("/api/recipes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("Title is required")));
        }

        @Test
        void missingRequiredQueryParam_returns400() throws Exception {
            mockMvc.perform(get("/api/recipes/search"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("query")));
        }

        @Test
        void malformedJsonBody_returns400() throws Exception {
            mockMvc.perform(post("/api/recipes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ not valid json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Malformed JSON request body"));
        }

        @Test
        void pathVariableTypeMismatch_returns400() throws Exception {
            mockMvc.perform(get("/api/recipes/abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("id")))
                    .andExpect(jsonPath("$.message").value(containsString("Long")));
        }

        @Test
        void unsupportedHttpMethod_returns405() throws Exception {
            mockMvc.perform(patch("/api/recipes/1"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.message").value(containsString("PATCH")));
        }

        @Test
        void unsupportedMediaType_returns415() throws Exception {
            mockMvc.perform(post("/api/recipes")
                            .contentType(MediaType.APPLICATION_XML)
                            .content("<recipe/>"))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        void illegalArgumentFromService_returns400() throws Exception {
            MockMultipartFile file = new MockMultipartFile("files", "a.pdf", "application/pdf", new byte[]{1});
            when(recipeService.uploadImages(eq(1L), anyList()))
                    .thenThrow(new IllegalArgumentException("Invalid file type. Allowed types: JPEG, PNG, WEBP, GIF"));

            mockMvc.perform(multipart("/api/recipes/1/images").file(file))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("Invalid file type")));
        }

        @Test
        void unexpectedException_returns500WithGenericMessageOnly() throws Exception {
            when(recipeService.getById(1L)).thenThrow(new RuntimeException("db connection refused"));

            mockMvc.perform(get("/api/recipes/1"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
        }

        @Test
        void recipeNotFound_onCreate_returns404() throws Exception {
            when(commentService.create(eq(99L), any())).thenThrow(new RecipeNotFoundException(99L));

            mockMvc.perform(post("/api/recipes/99/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CommentRequestDto("hi", null))))
                    .andExpect(status().isNotFound());
        }

        @Test
        void parentFromDifferentRecipe_returns400() throws Exception {
            when(commentService.create(eq(1L), any()))
                    .thenThrow(new IllegalArgumentException("Parent comment does not belong to this recipe"));

            mockMvc.perform(post("/api/recipes/1/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CommentRequestDto("reply", 1L))))
                    .andExpect(status().isBadRequest());
        }
    }

    private RecipeRequestDto validRequestDto() {
        RecipeRequestDto dto = new RecipeRequestDto();
        dto.setTitle("Waffles");
        Ingredient ingredient = new Ingredient();
        ingredient.setName("Flour");
        dto.setIngredients(List.of(ingredient));
        dto.setSteps(List.of(new Step("Mix", 1), new Step("Cook", 2)));
        return dto;
    }
}
