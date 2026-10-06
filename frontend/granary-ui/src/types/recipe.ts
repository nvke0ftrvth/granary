// Must match RecipeService.MAX_IMAGES_PER_RECIPE / MAX_IMAGE_SIZE_BYTES on the backend
export const MAX_IMAGES_PER_RECIPE = 3;
export const MAX_IMAGE_SIZE_BYTES = 2 * 1024 * 1024; // 2MB
export const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];

// Mirrors com.example.granary.dto.IngredientSuggestionDto
export interface IngredientSuggestion {
  id: number;
  name: string;
}

// Mirrors com.example.granary.dto.RecipeIngredientRequestDto
export interface RecipeIngredientRequest {
  suggestionId: number;
  measurement?: string;
  quantity?: number;
  optional?: boolean;
}

// Mirrors com.example.granary.dto.RecipeIngredientResponseDto
export interface RecipeIngredient {
  suggestionId: number;
  name: string;
  measurement?: string;
  quantity?: number;
  optional: boolean;
}

// Mirrors com.example.granary.model.Step
export interface Step {
  instruction: string;
  order: number;
}

// Mirrors com.example.granary.dto.RecipeRequestDto
export interface RecipeRequestDto {
  title: string;
  description?: string;
  ingredients: RecipeIngredientRequest[];
  steps: Step[];
  tags?: string[];
  prepTime?: string;
}

// Mirrors com.example.granary.dto.RecipeImageDto
export interface RecipeImage {
  id: number;
  imageUrl: string;
  displayOrder: number;
}

// Mirrors com.example.granary.dto.RecipeResponseDto (fields we use)
export interface RecipeResponseDto {
  id: number;
  title: string;
  description?: string;
  ingredients: RecipeIngredient[];
  steps: Step[];
  images?: RecipeImage[];
  tags?: string[];
  prepTime?: string;
  ownerUsername?: string;
  updated?: string;
  bookmarkCount?: number;
}

// Mirrors com.example.granary.dto.PageResponseDto
export interface PageResponseDto<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
