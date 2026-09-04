import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { RecipeRequestDto, RecipeResponseDto } from '../types/recipe';

export const recipeApi = createApi({
  reducerPath: 'recipeApi',
  baseQuery: fetchBaseQuery({ baseUrl: '/api/recipes' }),
  tagTypes: ['Recipe'],
  endpoints: (builder) => ({
    getRecipes: builder.query<RecipeResponseDto[], void>({
      query: () => '',
      providesTags: (result) =>
        result
          ? [
              ...result.map(({ id }) => ({ type: 'Recipe' as const, id })),
              { type: 'Recipe', id: 'LIST' },
            ]
          : [{ type: 'Recipe', id: 'LIST' }],
    }),
    getRecipeById: builder.query<RecipeResponseDto, number>({
      query: (id) => `/${id}`,
      providesTags: (_result, _error, id) => [{ type: 'Recipe', id }],
    }),
    createRecipe: builder.mutation<RecipeResponseDto, RecipeRequestDto>({
      query: (body) => ({ url: '', method: 'POST', body }),
      invalidatesTags: [{ type: 'Recipe', id: 'LIST' }],
    }),
    updateRecipe: builder.mutation<RecipeResponseDto, { id: number; body: RecipeRequestDto }>({
      query: ({ id, body }) => ({ url: `/${id}`, method: 'PUT', body }),
      invalidatesTags: (_result, _error, { id }) => [
        { type: 'Recipe', id },
        { type: 'Recipe', id: 'LIST' },
      ],
    }),
  }),
});

export const {
  useGetRecipesQuery,
  useGetRecipeByIdQuery,
  useCreateRecipeMutation,
  useUpdateRecipeMutation,
} = recipeApi;
