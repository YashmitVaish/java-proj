import { useState } from 'react';
import { Field, FormError } from '../components/ui.jsx';
import { api, ApiError } from '../lib/api.js';
import { useTitle } from '../lib/hooks.js';
import { useAuth } from '../state.jsx';

export default function Settings() {
  useTitle('Settings');
  const { user, setUser, signOut } = useAuth();

  return (
    <>
      <div className="page-head">
        <h1>Settings</h1>
      </div>

      <div className="settings">
        <section aria-labelledby="set-profile">
          <h2 id="set-profile" className="section-title">
            Profile
          </h2>
          {user ? <ProfileForm user={user} onSaved={setUser} /> : <p className="loading">Loading…</p>}
        </section>

        <section aria-labelledby="set-password">
          <h2 id="set-password" className="section-title">
            Password
          </h2>
          <PasswordForm />
        </section>

        <section aria-labelledby="set-session">
          <h2 id="set-session" className="section-title">
            Session
          </h2>
          <p className="muted">You stay logged in on this browser for 24 hours.</p>
          <button type="button" className="btn" onClick={signOut}>
            Log out
          </button>
        </section>
      </div>
    </>
  );
}

function ProfileForm({ user, onSaved }) {
  const [name, setName] = useState(user.name);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [saved, setSaved] = useState(false);

  async function save(e) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setSaved(false);
    try {
      onSaved(await api.updateMe({ name }));
      setSaved(true);
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="stack narrow" onSubmit={save} noValidate>
      <Field label="Name" error={error?.fieldErrors?.name}>
        <input type="text" autoComplete="name" value={name} onChange={(e) => (setName(e.target.value), setSaved(false))} />
      </Field>
      <Field label="Email" hint="Your email can’t be changed.">
        <input type="email" value={user.email} readOnly />
      </Field>
      <FormError error={error} />
      <div className="form-foot">
        <button type="submit" className="btn btn-primary" disabled={busy || !name.trim() || name.trim() === user.name}>
          {busy ? 'Saving…' : 'Save name'}
        </button>
        {saved && (
          <span className="saved" role="status">
            Name saved
          </span>
        )}
      </div>
    </form>
  );
}

function PasswordForm() {
  const [currentPassword, setCurrent] = useState('');
  const [newPassword, setNew] = useState('');
  const [confirm, setConfirm] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [saved, setSaved] = useState(false);

  async function save(e) {
    e.preventDefault();
    setSaved(false);
    if (newPassword !== confirm) {
      setError(new ApiError(400, 'Validation failed', { confirm: 'Doesn’t match the new password' }));
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await api.changePassword({ currentPassword, newPassword });
      setCurrent('');
      setNew('');
      setConfirm('');
      setSaved(true);
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="stack narrow" onSubmit={save} noValidate>
      <Field label="Current password" error={error?.fieldErrors?.currentPassword}>
        <input type="password" autoComplete="current-password" value={currentPassword} onChange={(e) => setCurrent(e.target.value)} />
      </Field>
      <Field label="New password" hint="At least 8 characters" error={error?.fieldErrors?.newPassword}>
        <input type="password" autoComplete="new-password" value={newPassword} onChange={(e) => setNew(e.target.value)} />
      </Field>
      <Field label="Confirm new password" error={error?.fieldErrors?.confirm}>
        <input type="password" autoComplete="new-password" value={confirm} onChange={(e) => setConfirm(e.target.value)} />
      </Field>
      <FormError error={error} />
      <div className="form-foot">
        <button type="submit" className="btn btn-primary" disabled={busy || !currentPassword || !newPassword}>
          {busy ? 'Changing…' : 'Change password'}
        </button>
        {saved && (
          <span className="saved" role="status">
            Password changed
          </span>
        )}
      </div>
    </form>
  );
}
