import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { RecipeResponseDto } from '../types/recipe';
import type { UserProfileDto } from '../types/user';

// Public endpoints -- no auth header needed.
export const userApi = createApi({
  reducerPath: 'userApi',
  baseQuery: fetchBaseQuery({ baseUrl: '/api/users' }),
  tagTypes: ['UserProfile', 'UserRecipes'],
  endpoints: (builder) => ({
    getUserProfile: builder.query<UserProfileDto, string>({
      query: (username) => `/${username}`,
      providesTags: (_result, _error, username) => [{ type: 'UserProfile', id: username }],
    }),
    getUserRecipes: builder.query<RecipeResponseDto[], string>({
      query: (username) => `/${username}/recipes`,
      providesTags: (_result, _error, username) => [{ type: 'UserRecipes', id: username }],
    }),
  }),
});

export const { useGetUserProfileQuery, useGetUserRecipesQuery } = userApi;
