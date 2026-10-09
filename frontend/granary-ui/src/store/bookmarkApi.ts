import { createApi } from '@reduxjs/toolkit/query/react';
import type { RecipeResponseDto } from '../types/recipe';
import { authedBaseQuery } from './authedBaseQuery';
import { recipeApi } from './recipeApi';

export const bookmarkApi = createApi({
  reducerPath: 'bookmarkApi',
  baseQuery: authedBaseQuery('/api'),
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
      async onQueryStarted(recipeId, { dispatch, queryFulfilled }) {
        await queryFulfilled;
        dispatch(
          recipeApi.util.invalidateTags([
            { type: 'Recipe', id: 'POPULAR' },
            { type: 'Recipe', id: 'LIST' },
            { type: 'Recipe', id: recipeId },
          ])
        );
      },
    }),
    removeBookmark: builder.mutation<void, number>({
      query: (recipeId) => ({ url: `recipes/${recipeId}/bookmark`, method: 'DELETE' }),
      invalidatesTags: [{ type: 'Bookmark', id: 'LIST' }],
      async onQueryStarted(recipeId, { dispatch, queryFulfilled }) {
        await queryFulfilled;
        dispatch(
          recipeApi.util.invalidateTags([
            { type: 'Recipe', id: 'POPULAR' },
            { type: 'Recipe', id: 'LIST' },
            { type: 'Recipe', id: recipeId },
          ])
        );
      },
    }),
  }),
});

export const { useGetMyBookmarksQuery, useAddBookmarkMutation, useRemoveBookmarkMutation } =
  bookmarkApi;
