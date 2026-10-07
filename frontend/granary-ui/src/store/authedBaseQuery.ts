import { fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { BaseQueryFn, FetchArgs, FetchBaseQueryError } from '@reduxjs/toolkit/query/react';
import { expireSession } from './authSlice';
import type { RootState } from './index';

/** fetchBaseQuery that sends the stored token and ends the session when the server rejects it with a 401. */
export function authedBaseQuery(baseUrl: string): BaseQueryFn<string | FetchArgs, unknown, FetchBaseQueryError> {
  const rawBaseQuery = fetchBaseQuery({
    baseUrl,
    prepareHeaders: (headers, { getState }) => {
      const token = (getState() as RootState).auth.token;
      if (token) {
        headers.set('Authorization', `Bearer ${token}`);
      }
      return headers;
    },
  });

  return async (args, api, extraOptions) => {
    const tokenSent = (api.getState() as RootState).auth.token;
    const result = await rawBaseQuery(args, api, extraOptions);

    if (result.error?.status === 401 && tokenSent && (api.getState() as RootState).auth.token === tokenSent) {
      api.dispatch(expireSession());
    }
    return result;
  };
}
