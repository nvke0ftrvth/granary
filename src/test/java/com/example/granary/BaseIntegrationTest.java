package com.example.granary;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.granary.dto.RecipeRequestDto;
import com.example.granary.model.Ingredient;
import com.example.granary.model.Step;
import com.example.granary.repo.RecipeImageRepository;
import com.example.granary.repo.RecipeRepository;

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

    @BeforeEach
    void clearDatabase() {
        recipeImageRepository.deleteAll();
        recipeRepository.deleteAll();
    }

    protected RecipeRequestDto buildRecipeRequest(String title) {
        Ingredient ingredient1 = new Ingredient();
        ingredient1.setName("Ingredient 1");
        ingredient1.setQuantity(1);
        ingredient1.setMeasurement("gram");

        Ingredient ingredient2 = new Ingredient();
        ingredient2.setName("Ingredient 2");
        ingredient2.setQuantity(2);
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