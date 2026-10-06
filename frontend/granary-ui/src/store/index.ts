import { configureStore, createListenerMiddleware, isAnyOf } from '@reduxjs/toolkit';
import { recipeApi } from './recipeApi';
import { authApi } from './authApi';
import { bookmarkApi } from './bookmarkApi';
import { commentApi } from './commentApi';
import { userApi } from './userApi';
import { ingredientSuggestionApi } from './ingredientSuggestionApi';
import authReducer, { logout, setCredentials } from './authSlice';

const authChangeListener = createListenerMiddleware();
authChangeListener.startListening({
  matcher: isAnyOf(setCredentials, logout),
  effect: (_action, api) => {
    api.dispatch(bookmarkApi.util.resetApiState());
    api.dispatch(commentApi.util.resetApiState());
    api.dispatch(recipeApi.util.invalidateTags([{ type: 'Recipe', id: 'MINE' }]));
  },
});

export const store = configureStore({
  reducer: {
    [recipeApi.reducerPath]: recipeApi.reducer,
    [authApi.reducerPath]: authApi.reducer,
    [bookmarkApi.reducerPath]: bookmarkApi.reducer,
    [commentApi.reducerPath]: commentApi.reducer,
    [userApi.reducerPath]: userApi.reducer,
    [ingredientSuggestionApi.reducerPath]: ingredientSuggestionApi.reducer,
    auth: authReducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().prepend(authChangeListener.middleware).concat(
      recipeApi.middleware,
      authApi.middleware,
      bookmarkApi.middleware,
      commentApi.middleware,
      userApi.middleware,
      ingredientSuggestionApi.middleware
    ),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
