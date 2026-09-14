import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { RecipeResponseDto } from '../types/recipe';
import type { RootState } from './index';

export const bookmarkApi = createApi({
  reducerPath: 'bookmarkApi',
  baseQuery: fetchBaseQuery({
    baseUrl: '/api',
    prepareHeaders: (headers, { getState }) => {
      const token = (getState() as RootState).auth.token;
      if (token) {
        headers.set('Authorization', `Bearer ${token}`);
      }
      return headers;
    },
  }),
  tagTypes: ['Bookmark'],
  endpoints: (builder) => ({
    getMyBookmarks: builder.query<RecipeResponseDto[], void>({
      query: () => 'bookmarks',
      providesTags: (result) =>
        result
          ? [
              ...result.map(({ id }) => ({ type: 'Bookmark' as const, id })),
              { type: 'Bookmark', id: 'LIST' },
            ]
          : [{ type: 'Bookmark', id: 'LIST' }],
    }),
    addBookmark: builder.mutation<void, number>({
      query: (recipeId) => ({ url: `recipes/${recipeId}/bookmark`, method: 'POST' }),
      invalidatesTags: [{ type: 'Bookmark', id: 'LIST' }],
    }),
    removeBookmark: builder.mutation<void, number>({
      query: (recipeId) => ({ url: `recipes/${recipeId}/bookmark`, method: 'DELETE' }),
      invalidatesTags: [{ type: 'Bookmark', id: 'LIST' }],
    }),
  }),
});

export const { useGetMyBookmarksQuery, useAddBookmarkMutation, useRemoveBookmarkMutation } =
  bookmarkApi;
