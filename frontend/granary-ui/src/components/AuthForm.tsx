import { useState } from 'react';
import { useDispatch } from 'react-redux';
import { useLoginMutation, useRegisterMutation } from '../store/authApi';
import { setCredentials } from '../store/authSlice';

type Mode = 'login' | 'register';

interface AuthFormProps {
  onSuccess?: () => void;
}

export function AuthForm({ onSuccess }: AuthFormProps) {
  const [mode, setMode] = useState<Mode>('login');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const dispatch = useDispatch();
  const [login, loginState] = useLoginMutation();
  const [register, registerState] = useRegisterMutation();

  const isRegister = mode === 'register';
  const { isLoading, error } = isRegister ? registerState : loginState;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const result = isRegister
        ? await register({ username: username.trim(), email: email.trim(), password }).unwrap()
        : await login({ username: username.trim(), password }).unwrap();

      dispatch(setCredentials({ token: result.token, username: result.username }));
      onSuccess?.();
    } catch {
      // error surfaced below via the `error` state
    }
  };

  const errorMessage = () => {
    if (!error) return null;
    // RTK Query errors from our backend carry { data: { message } }
    if ('data' in error && error.data && typeof error.data === 'object' && 'message' in error.data) {
      return String((error.data as { message: unknown }).message);
    }
    return isRegister ? "Couldn't create that account." : "Couldn't log you in.";
  };

  return (
    <div className="auth-form-wrapper">
      <form className="recipe-form auth-form" onSubmit={handleSubmit}>
        <h2 className="auth-title">{isRegister ? 'Create an account' : 'Welcome back'}</h2>

        <label className="field">
          <span className="field-label">Username</span>
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="yourname"
            autoComplete="username"
            required
          />
        </label>

        {isRegister && (
          <label className="field">
            <span className="field-label">Email</span>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              autoComplete="email"
              required
            />
          </label>
        )}

        <label className="field">
          <span className="field-label">Password</span>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder={isRegister ? 'At least 8 characters' : '••••••••'}
            autoComplete={isRegister ? 'new-password' : 'current-password'}
            required
          />
        </label>

        {error && <p className="form-error">{errorMessage()}</p>}

        <button type="submit" className="submit-btn" disabled={isLoading}>
          {isLoading ? 'Please wait…' : isRegister ? 'Create account' : 'Log in'}
        </button>

        <button
          type="button"
          className="auth-mode-toggle"
          onClick={() => setMode(isRegister ? 'login' : 'register')}
        >
          {isRegister ? 'Already have an account? Log in' : "Don't have an account? Register"}
        </button>
      </form>
    </div>
  );
}
