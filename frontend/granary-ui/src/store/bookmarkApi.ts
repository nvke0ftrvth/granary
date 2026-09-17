import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { RecipeResponseDto } from '../types/recipe';
import type { RootState } from './index';
import { recipeApi } from './recipeApi';

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
      async onQueryStarted(_recipeId, { dispatch, queryFulfilled }) {
        await queryFulfilled;
        dispatch(recipeApi.util.invalidateTags([{ type: 'Recipe', id: 'POPULAR' }]));
      },
    }),
    removeBookmark: builder.mutation<void, number>({
      query: (recipeId) => ({ url: `recipes/${recipeId}/bookmark`, method: 'DELETE' }),
      invalidatesTags: [{ type: 'Bookmark', id: 'LIST' }],
      async onQueryStarted(_recipeId, { dispatch, queryFulfilled }) {
        await queryFulfilled;
        dispatch(recipeApi.util.invalidateTags([{ type: 'Recipe', id: 'POPULAR' }]));
      },
    }),
  }),
});

export const { useGetMyBookmarksQuery, useAddBookmarkMutation, useRemoveBookmarkMutation } =
  bookmarkApi;
