import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';

const TOKEN_KEY = 'granary_token';
const USERNAME_KEY = 'granary_username';

interface AuthState {
  token: string | null;
  username: string | null;
}

const initialState: AuthState = {
  token: localStorage.getItem(TOKEN_KEY),
  username: localStorage.getItem(USERNAME_KEY),
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials: (state, action: PayloadAction<{ token: string; username: string }>) => {
      state.token = action.payload.token;
      state.username = action.payload.username;
      localStorage.setItem(TOKEN_KEY, action.payload.token);
      localStorage.setItem(USERNAME_KEY, action.payload.username);
    },
    logout: (state) => {
      state.token = null;
      state.username = null;
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USERNAME_KEY);
    },
  },
});

export const { setCredentials, logout } = authSlice.actions;
export default authSlice.reducer;
