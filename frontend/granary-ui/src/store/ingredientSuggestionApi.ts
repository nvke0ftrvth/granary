import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { IngredientSuggestion } from '../types/recipe';

export const INGREDIENT_SEARCH_MIN_LENGTH = 2;
export const INGREDIENT_SEARCH_LIMIT = 10;

export const ingredientSuggestionApi = createApi({
  reducerPath: 'ingredientSuggestionApi',
  baseQuery: fetchBaseQuery({ baseUrl: '/api/ingredients' }),
  endpoints: (builder) => ({
    searchIngredientSuggestions: builder.query<IngredientSuggestion[], string>({
      query: (query) => ({ url: '/search', params: { query, limit: INGREDIENT_SEARCH_LIMIT } }),
      keepUnusedDataFor: 300,
    }),
  }),
});

export const { useSearchIngredientSuggestionsQuery } = ingredientSuggestionApi;
