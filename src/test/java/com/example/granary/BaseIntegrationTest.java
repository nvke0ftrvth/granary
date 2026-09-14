package com.example.granary;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.example.granary.dto.AuthResponseDto;
import com.example.granary.dto.RecipeRequestDto;
import com.example.granary.dto.RegisterRequestDto;
import com.example.granary.model.Ingredient;
import com.example.granary.model.Step;
import com.example.granary.repo.RecipeImageRepository;
import com.example.granary.repo.RecipeRepository;
import com.example.granary.repo.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected RecipeRepository recipeRepository;

    @Autowired
    protected RecipeImageRepository recipeImageRepository;

    @Autowired
    protected UserRepository userRepository;


    @BeforeEach
    void clearDatabase() {
        recipeImageRepository.deleteAll();
        recipeRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected String registerAndGetToken(String username) {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setUsername(username);
        request.setEmail(username + "@test.com");
        request.setPassword("password123");

        ResponseEntity<AuthResponseDto> response = restTemplate.postForEntity(
                authUrl() + "/register", request, AuthResponseDto.class);

        return response.getBody().getToken();
    }


    protected HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // Build a request entity with auth headers
    protected <T> HttpEntity<T> authEntity(T body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Temporary debug
        System.out.println("Sending token: " + token);
        System.out.println("Headers: " + headers);

        return new HttpEntity<>(body, authHeaders(token));
    }

    protected abstract String baseUrl();

    protected String authUrl() {
        return "http://localhost:" + port + "/api/auth";
    }

    @LocalServerPort
    protected int port;

    protected RecipeRequestDto buildRecipeRequest(String title) {
        Ingredient ingredient1 = new Ingredient();
        ingredient1.setName("Ingredient 1");
        ingredient1.setQuantity(BigDecimal.valueOf(1.0));
        ingredient1.setMeasurement("gram");

        Ingredient ingredient2 = new Ingredient();
        ingredient2.setName("Ingredient 2");
        ingredient2.setQuantity(BigDecimal.valueOf(2.0));
        ingredient2.setMeasurement("kilogram");

        RecipeRequestDto dto = new RecipeRequestDto();
        dto.setTitle(title);
        dto.setDescription("A test recipe description");
        dto.setIngredients(List.of(ingredient1, ingredient2));
        dto.setSteps(List.of(new Step("Step 1", 1), new Step("Step 2", 2)));
        dto.setTags(List.of("test", "quick"));
        dto.setPrepTime("2 Minutes");
        return dto;
    }
}