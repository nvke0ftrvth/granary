import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';

const TOKEN_KEY = 'granary_token';
const USERNAME_KEY = 'granary_username';
const ROLE_KEY = 'granary_role';

interface AuthState {
  token: string | null;
  username: string | null;
  role: string | null;
  sessionExpired: boolean;
}

const initialState: AuthState = {
  token: localStorage.getItem(TOKEN_KEY),
  username: localStorage.getItem(USERNAME_KEY),
  role: localStorage.getItem(ROLE_KEY),
  sessionExpired: false,
};

function clearCredentials(state: AuthState) {
  state.token = null;
  state.username = null;
  state.role = null;
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USERNAME_KEY);
  localStorage.removeItem(ROLE_KEY);
}

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials: (state, action: PayloadAction<{ token: string; username: string; role: string }>) => {
      state.token = action.payload.token;
      state.username = action.payload.username;
      state.role = action.payload.role;
      state.sessionExpired = false;
      localStorage.setItem(TOKEN_KEY, action.payload.token);
      localStorage.setItem(USERNAME_KEY, action.payload.username);
      localStorage.setItem(ROLE_KEY, action.payload.role);
    },
    logout: (state) => {
      clearCredentials(state);
      state.sessionExpired = false;
    },
    /** The server rejected the stored token, so the user is logged out and should be sent to log in again. */
    expireSession: (state) => {
      clearCredentials(state);
      state.sessionExpired = true;
    },
    sessionExpiryHandled: (state) => {
      state.sessionExpired = false;
    },
  },
});

export const { setCredentials, logout, expireSession, sessionExpiryHandled } = authSlice.actions;
export default authSlice.reducer;
