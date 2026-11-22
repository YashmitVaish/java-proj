import { useState } from 'react';
import { ConfirmDialog, FormError, Swatch } from '../components/ui.jsx';
import { api } from '../lib/api.js';
import { useTitle } from '../lib/hooks.js';
import { useApp } from '../state.jsx';

export default function Categories() {
  useTitle('Categories');
  const { categories, refresh, colorOf } = useApp();
  const [deleting, setDeleting] = useState(null);

  return (
    <>
      <div className="page-head">
        <h1>Categories</h1>
      </div>
      <p className="page-intro">
        Every transaction belongs to one category. Uncategorized is built in and catches transactions from deleted
        categories.
      </p>

      <div className="split">
        {[
          { type: 'EXPENSE', title: 'Money out' },
          { type: 'INCOME', title: 'Money in' },
        ].map(({ type, title }) => (
          <section key={type} aria-labelledby={`cat-${type}`}>
            <h2 id={`cat-${type}`} className="section-title">
              {title}
            </h2>
            <AddCategory type={type} onAdded={refresh} />
            <ul className="cat-list">
              {categories
                .filter((c) => c.type === type)
                .map((c) => (
                  <CategoryRow key={c.id} category={c} color={colorOf(c.id, type)} onDelete={() => setDeleting(c)} onSaved={refresh} />
                ))}
            </ul>
          </section>
        ))}
      </div>

      {deleting && (
        <ConfirmDialog
          title={`Delete ${deleting.name}?`}
          confirmLabel="Delete category"
          onClose={() => setDeleting(null)}
          onConfirm={async () => {
            await api.deleteCategory(deleting.id);
            refresh();
          }}
        >
          <p>
            Its transactions move to Uncategorized{deleting.type === 'EXPENSE' && ', and any budgets set for it are deleted'}.
          </p>
        </ConfirmDialog>
      )}
    </>
  );
}

function AddCategory({ type, onAdded }) {
  const [name, setName] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  async function add(e) {
    e.preventDefault();
    if (!name.trim()) return;
    setBusy(true);
    setError(null);
    try {
      await api.createCategory({ name: name.trim(), type });
      setName('');
      onAdded();
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }

  const label = type === 'EXPENSE' ? 'New spending category' : 'New income category';
  return (
    <form className="add-row" onSubmit={add} noValidate>
      <label className="sr-only" htmlFor={`new-${type}`}>
        {label}
      </label>
      <input
        id={`new-${type}`}
        type="text"
        placeholder={type === 'EXPENSE' ? 'e.g. Mess fees' : 'e.g. Stipend'}
        value={name}
        maxLength={60}
        onChange={(e) => setName(e.target.value)}
      />
      <button type="submit" className="btn" disabled={busy || !name.trim()}>
        Add
      </button>
      <FormError error={error} />
    </form>
  );
}

function CategoryRow({ category: c, color, onDelete, onSaved }) {
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState(c.name);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  async function save(e) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await api.renameCategory(c.id, name.trim());
      setEditing(false);
      onSaved();
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }

  if (editing) {
    return (
      <li className="cat-row is-editing">
        <form className="add-row" onSubmit={save} noValidate>
          <label className="sr-only" htmlFor={`rename-${c.id}`}>
            New name for {c.name}
          </label>
          <input id={`rename-${c.id}`} type="text" autoFocus value={name} maxLength={60} onChange={(e) => setName(e.target.value)} />
          <button type="submit" className="btn btn-primary" disabled={busy || !name.trim()}>
            Save
          </button>
          <button
            type="button"
            className="btn"
            onClick={() => {
              setEditing(false);
              setName(c.name);
              setError(null);
            }}
          >
            Cancel
          </button>
          <FormError error={error} />
        </form>
      </li>
    );
  }

  return (
    <li className="cat-row">
      <Swatch color={color} />
      <span className="cat-name">{c.name}</span>
      {c.system ? (
        <span className="muted small">Built in</span>
      ) : (
        <span className="row-actions">
          <button type="button" className="btn-link" onClick={() => setEditing(true)}>
            Rename<span className="sr-only"> {c.name}</span>
          </button>
          <button type="button" className="btn-link danger" onClick={onDelete}>
            Delete<span className="sr-only"> {c.name}</span>
          </button>
        </span>
      )}
    </li>
  );
}
