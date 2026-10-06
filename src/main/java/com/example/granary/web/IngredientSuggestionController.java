package com.example.granary.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.granary.business.IngredientSuggestionService;
import com.example.granary.dto.IngredientSuggestionDto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
@Validated
public class IngredientSuggestionController {

    private final IngredientSuggestionService suggestionService;

    @GetMapping("/search")
    public ResponseEntity<List<IngredientSuggestionDto>> search(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "10") @Min(1) @Max(IngredientSuggestionService.MAX_LIMIT) int limit) {
        return ResponseEntity.ok(suggestionService.search(query, limit));
    }
}
