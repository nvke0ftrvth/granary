import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { RecipeResponseDto } from '../types/recipe';
import type { UserProfileDto } from '../types/user';
import type { RootState } from './index';

// GETs are public; mutations (profile/avatar edits) require the JWT.
export const userApi = createApi({
  reducerPath: 'userApi',
  baseQuery: fetchBaseQuery({
    baseUrl: '/api/users',
    prepareHeaders: (headers, { getState }) => {
      const token = (getState() as RootState).auth.token;
      if (token) {
        headers.set('Authorization', `Bearer ${token}`);
      }
      return headers;
    },
  }),
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
    updateDescription: builder.mutation<UserProfileDto, { username: string; description: string }>({
      query: ({ username, description }) => ({
        url: `/${username}`,
        method: 'PUT',
        body: { description },
      }),
      invalidatesTags: (_result, _error, { username }) => [{ type: 'UserProfile', id: username }],
    }),
    uploadAvatar: builder.mutation<UserProfileDto, { username: string; file: File }>({
      query: ({ username, file }) => {
        const formData = new FormData();
        formData.append('file', file);
        return { url: `/${username}/avatar`, method: 'POST', body: formData };
      },
      invalidatesTags: (_result, _error, { username }) => [{ type: 'UserProfile', id: username }],
    }),
    deleteAvatar: builder.mutation<UserProfileDto, string>({
      query: (username) => ({ url: `/${username}/avatar`, method: 'DELETE' }),
      invalidatesTags: (_result, _error, username) => [{ type: 'UserProfile', id: username }],
    }),
  }),
});

export const {
  useGetUserProfileQuery,
  useGetUserRecipesQuery,
  useUpdateDescriptionMutation,
  useUploadAvatarMutation,
  useDeleteAvatarMutation,
} = userApi;
