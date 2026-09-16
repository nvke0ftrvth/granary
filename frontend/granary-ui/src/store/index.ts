import { configureStore } from '@reduxjs/toolkit';
import { recipeApi } from './recipeApi';
import { authApi } from './authApi';
import { bookmarkApi } from './bookmarkApi';
import { commentApi } from './commentApi';
import { userApi } from './userApi';
import authReducer from './authSlice';

export const store = configureStore({
  reducer: {
    [recipeApi.reducerPath]: recipeApi.reducer,
    [authApi.reducerPath]: authApi.reducer,
    [bookmarkApi.reducerPath]: bookmarkApi.reducer,
    [commentApi.reducerPath]: commentApi.reducer,
    [userApi.reducerPath]: userApi.reducer,
    auth: authReducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().concat(
      recipeApi.middleware,
      authApi.middleware,
      bookmarkApi.middleware,
      commentApi.middleware,
      userApi.middleware
    ),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
