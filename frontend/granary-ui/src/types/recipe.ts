// Mirrors com.example.granary.model.Ingredient
export interface Ingredient {
  id?: number;
  name: string;
  measurement?: string;
  quantity?: number;
}

// Mirrors com.example.granary.model.Step
export interface Step {
  instruction: string;
  order: number;
}

// Mirrors com.example.granary.dto.RecipeRequestDto
// (text-only for now: no images, no prepTime/user wiring yet)
export interface RecipeRequestDto {
  title: string;
  description?: string;
  ingredients: Ingredient[];
  optionalIngredients?: Ingredient[];
  steps: Step[];
  tags?: string[];
}

// Mirrors com.example.granary.dto.RecipeResponseDto (fields we use)
export interface RecipeResponseDto {
  id: number;
  title: string;
  description?: string;
  ingredients: Ingredient[];
  optionalIngredients?: Ingredient[];
  steps: Step[];
  tags?: string[];
  updated?: string;
}
