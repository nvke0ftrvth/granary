import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { CommentRequestDto, CommentResponseDto } from '../types/comment';
import type { RootState } from './index';

export const commentApi = createApi({
  reducerPath: 'commentApi',
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
  tagTypes: ['Comment'],
  endpoints: (builder) => ({
    getComments: builder.query<CommentResponseDto[], number>({
      query: (recipeId) => `recipes/${recipeId}/comments`,
      providesTags: (_result, _error, recipeId) => [{ type: 'Comment', id: `RECIPE_${recipeId}` }],
    }),
    getMyComments: builder.query<CommentResponseDto[], void>({
      query: () => 'comments/mine',
      providesTags: [{ type: 'Comment', id: 'MINE' }],
    }),
    createComment: builder.mutation<CommentResponseDto, { recipeId: number; body: CommentRequestDto }>({
      query: ({ recipeId, body }) => ({ url: `recipes/${recipeId}/comments`, method: 'POST', body }),
      invalidatesTags: (_result, _error, { recipeId }) => [{ type: 'Comment', id: `RECIPE_${recipeId}` }],
    }),
    updateComment: builder.mutation<
      CommentResponseDto,
      { id: number; recipeId: number; body: CommentRequestDto }
    >({
      query: ({ id, body }) => ({ url: `comments/${id}`, method: 'PUT', body }),
      invalidatesTags: (_result, _error, { recipeId }) => [{ type: 'Comment', id: `RECIPE_${recipeId}` }],
    }),
    deleteComment: builder.mutation<void, { id: number; recipeId: number }>({
      query: ({ id }) => ({ url: `comments/${id}`, method: 'DELETE' }),
      invalidatesTags: (_result, _error, { recipeId }) => [{ type: 'Comment', id: `RECIPE_${recipeId}` }],
    }),
    voteComment: builder.mutation<CommentResponseDto, { id: number; recipeId: number; value: 1 | -1 }>({
      query: ({ id, value }) => ({ url: `comments/${id}/vote`, method: 'PUT', body: { value } }),
      invalidatesTags: (_result, _error, { recipeId }) => [{ type: 'Comment', id: `RECIPE_${recipeId}` }],
    }),
    removeVote: builder.mutation<void, { id: number; recipeId: number }>({
      query: ({ id }) => ({ url: `comments/${id}/vote`, method: 'DELETE' }),
      invalidatesTags: (_result, _error, { recipeId }) => [{ type: 'Comment', id: `RECIPE_${recipeId}` }],
    }),
  }),
});

export const {
  useGetCommentsQuery,
  useGetMyCommentsQuery,
  useCreateCommentMutation,
  useUpdateCommentMutation,
  useDeleteCommentMutation,
  useVoteCommentMutation,
  useRemoveVoteMutation,
} = commentApi;
