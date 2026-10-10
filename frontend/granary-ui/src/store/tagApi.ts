import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { TagDto } from '../types/tag';

export const TRENDING_TAGS_LIMIT = 12;

export const tagApi = createApi({
  reducerPath: 'tagApi',
  baseQuery: fetchBaseQuery({ baseUrl: '/api/tags' }),
  tagTypes: ['Tag'],
  endpoints: (builder) => ({
    getTags: builder.query<TagDto[], void>({
      query: () => '',
      providesTags: ['Tag'],
      keepUnusedDataFor: 300,
    }),
    getPopularTags: builder.query<TagDto[], number | void>({
      query: (limit) => ({ url: '/popular', params: { limit: limit ?? TRENDING_TAGS_LIMIT } }),
      providesTags: ['Tag'],
    }),
  }),
});

export const { useGetTagsQuery, useGetPopularTagsQuery } = tagApi;
