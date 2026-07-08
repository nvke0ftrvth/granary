package com.example.granary.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.example.granary.model.Recipe;
import com.example.granary.model.RecipeImage;

@Mapper(componentModel = "spring")
public interface RecipeMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "updated", ignore = true)
    Recipe toEntity(RecipeRequestDto dto);

    RecipeResponseDto toResponseDto(Recipe recipe);

    RecipeImageDto toImageDto(RecipeImage image);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "updated", ignore = true)
    void updateEntityFromDto(RecipeRequestDto dto, @MappingTarget Recipe recipe);
}
