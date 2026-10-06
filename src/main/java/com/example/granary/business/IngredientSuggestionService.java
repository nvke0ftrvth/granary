package com.example.granary.business;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.example.granary.dto.IngredientSuggestionDto;
import com.example.granary.repo.IngredientSuggestionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IngredientSuggestionService {

    public static final int MIN_QUERY_LENGTH = 2;
    public static final int DEFAULT_LIMIT = 10;
    public static final int MAX_LIMIT = 50;

    private final IngredientSuggestionRepository suggestionRepository;

    /**
     * Type-ahead search over ingredient suggestions. Queries shorter than {@link #MIN_QUERY_LENGTH}
     * return an empty list rather than every suggestion.
     */
    public List<IngredientSuggestionDto> search(String query, int limit) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.length() < MIN_QUERY_LENGTH) {
            return List.of();
        }
        String term = escapeLike(trimmed.toLowerCase());
        return suggestionRepository.search(term, PageRequest.of(0, limit)).stream()
                .map(s -> new IngredientSuggestionDto(s.getId(), s.getName()))
                .toList();
    }

    private static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
