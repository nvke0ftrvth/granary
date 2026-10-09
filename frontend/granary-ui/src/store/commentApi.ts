import { createApi } from '@reduxjs/toolkit/query/react';
import type { CommentRequestDto, CommentResponseDto } from '../types/comment';
import { authedBaseQuery } from './authedBaseQuery';

export const commentApi = createApi({
  reducerPath: 'commentApi',
  baseQuery: authedBaseQuery('/api'),
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
      invalidatesTags: (_result, _error, { recipeId }) => [
        { type: 'Comment', id: `RECIPE_${recipeId}` },
        { type: 'Comment', id: 'MINE' },
      ],
    }),
    updateComment: builder.mutation<
      CommentResponseDto,
      { id: number; recipeId: number; body: CommentRequestDto }
    >({
      query: ({ id, body }) => ({ url: `comments/${id}`, method: 'PUT', body }),
      invalidatesTags: (_result, _error, { recipeId }) => [
        { type: 'Comment', id: `RECIPE_${recipeId}` },
        { type: 'Comment', id: 'MINE' },
      ],
    }),
    deleteComment: builder.mutation<void, { id: number; recipeId: number }>({
      query: ({ id }) => ({ url: `comments/${id}`, method: 'DELETE' }),
      invalidatesTags: (_result, _error, { recipeId }) => [
        { type: 'Comment', id: `RECIPE_${recipeId}` },
        { type: 'Comment', id: 'MINE' },
      ],
    }),
    voteComment: builder.mutation<CommentResponseDto, { id: number; recipeId: number; value: 1 | -1 }>({
      query: ({ id, value }) => ({ url: `comments/${id}/vote`, method: 'PUT', body: { value } }),
      invalidatesTags: (_result, _error, { recipeId }) => [
        { type: 'Comment', id: `RECIPE_${recipeId}` },
        { type: 'Comment', id: 'MINE' },
      ],
    }),
    removeVote: builder.mutation<void, { id: number; recipeId: number }>({
      query: ({ id }) => ({ url: `comments/${id}/vote`, method: 'DELETE' }),
      invalidatesTags: (_result, _error, { recipeId }) => [
        { type: 'Comment', id: `RECIPE_${recipeId}` },
        { type: 'Comment', id: 'MINE' },
      ],
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
