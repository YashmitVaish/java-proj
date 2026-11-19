import { useState } from 'react';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { Field, FormError } from '../components/ui.jsx';
import { api } from '../lib/api.js';
import { useTitle } from '../lib/hooks.js';
import { useAuth } from '../state.jsx';

export default function AuthPage({ mode }) {
  const isRegister = mode === 'register';
  useTitle(isRegister ? 'Create account' : 'Log in');
  const { token, signIn, sessionExpired } = useAuth();
  const location = useLocation();

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  if (token) return <Navigate to={location.state?.from || '/'} replace />;

  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const res = isRegister ? await api.register({ name, email, password }) : await api.login({ email, password });
      signIn(res);
    } catch (err) {
      setError(err);
      setBusy(false);
    }
  }

  const fieldError = (f) => error?.fieldErrors?.[f];

  return (
    <div className="auth">
      <div className="auth-panel">
        <p className="auth-wordmark" aria-hidden="true">
          Khata
        </p>
        <h1 className="auth-title">{isRegister ? 'Create an account' : 'Log in'}</h1>
        <p className="auth-lede">
          {isRegister
            ? 'Write down what comes in and what goes out, set monthly limits, and see where your rupees went.'
            : 'Log in to see this month’s spending.'}
        </p>

        {sessionExpired && !isRegister && !error && (
          <p className="notice" role="status">
            Your session expired after 24 hours. Log in again to carry on.
          </p>
        )}

        <form onSubmit={submit} className="stack" noValidate>
          {isRegister && (
            <Field label="Your name" error={fieldError('name')}>
              <input type="text" autoComplete="name" required value={name} onChange={(e) => setName(e.target.value)} />
            </Field>
          )}
          <Field label="Email" error={fieldError('email')}>
            <input type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
          </Field>
          <Field
            label="Password"
            error={fieldError('password')}
            hint={isRegister ? 'At least 8 characters' : undefined}
          >
            <input
              type="password"
              autoComplete={isRegister ? 'new-password' : 'current-password'}
              required
              minLength={isRegister ? 8 : undefined}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </Field>
          <FormError error={error} />
          <button type="submit" className="btn btn-primary btn-block" disabled={busy}>
            {busy ? (isRegister ? 'Creating account…' : 'Logging in…') : isRegister ? 'Create account' : 'Log in'}
          </button>
        </form>

        <p className="auth-switch">
          {isRegister ? (
            <>
              Already have an account? <Link to="/login">Log in</Link>
            </>
          ) : (
            <>
              New here? <Link to="/register">Create an account</Link>
            </>
          )}
        </p>
      </div>
    </div>
  );
}
