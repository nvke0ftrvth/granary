import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { AuthResponseDto, LoginRequestDto, RegisterRequestDto } from '../types/auth';

export const authApi = createApi({
  reducerPath: 'authApi',
  baseQuery: fetchBaseQuery({ baseUrl: '/api/auth' }),
  endpoints: (builder) => ({
    register: builder.mutation<AuthResponseDto, RegisterRequestDto>({
      query: (body) => ({ url: '/register', method: 'POST', body }),
    }),
    login: builder.mutation<AuthResponseDto, LoginRequestDto>({
      query: (body) => ({ url: '/login', method: 'POST', body }),
    }),
  }),
});

export const { useRegisterMutation, useLoginMutation } = authApi;
