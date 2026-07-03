package com.example.granary.business;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.granary.dto.RecipeMapper;
import com.example.granary.dto.RecipeRequestDto;
import com.example.granary.dto.RecipeResponseDto;
import com.example.granary.exceptions.RecipeNotFoundException;
import com.example.granary.model.Recipe;
import com.example.granary.repo.RecipeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeMapper recipeMapper;

    public RecipeResponseDto create(RecipeRequestDto dto) {
        Recipe recipe = recipeMapper.toEntity(dto);
        Recipe saved = recipeRepository.save(recipe);
        return recipeMapper.toResponseDto(saved);
    }

    public RecipeResponseDto getById(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));
        return recipeMapper.toResponseDto(recipe);
    }

    public List<RecipeResponseDto> getAll() {
        return recipeRepository.findAll()
                .stream()
                .map(recipeMapper::toResponseDto)
                .toList();
    }

    public RecipeResponseDto update(Long id, RecipeRequestDto dto) {
        Recipe existing = recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));
        recipeMapper.updateEntityFromDto(dto, existing); // updates in place
        return recipeMapper.toResponseDto(recipeRepository.save(existing));
    }

    public void delete(Long id) {
        recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException(id));
        recipeRepository.deleteById(id);
    }
}