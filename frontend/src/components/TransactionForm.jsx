import { useState } from 'react';
import { api, ApiError } from '../lib/api.js';
import { monthRange, today } from '../lib/format.js';
import { useApp } from '../state.jsx';
import { Field, FormError, Modal, Segmented } from './ui.jsx';

// New transactions default to today, or to the 1st of the month being viewed if that isn't this month.
function defaultDate(month) {
  const t = today();
  return t.startsWith(month) ? t : monthRange(month).from;
}

export default function TransactionForm({ tx, onClose }) {
  const { categories, refresh, month } = useApp();
  const isEdit = Boolean(tx?.id);

  const [type, setType] = useState(tx?.type || 'EXPENSE');
  const [amount, setAmount] = useState(tx?.amount != null ? String(tx.amount) : '');
  const [date, setDate] = useState(tx?.date || defaultDate(month));
  const [categoryId, setCategoryId] = useState(tx?.category?.id ? String(tx.category.id) : '');
  const [description, setDescription] = useState(tx?.description || '');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [confirmDelete, setConfirmDelete] = useState(false);

  const options = categories.filter((c) => c.type === type);
  const fieldError = (name) => error?.fieldErrors?.[name];

  function changeType(next) {
    setType(next);
    // a category only belongs to one type, so clear it if it no longer fits
    if (!categories.some((c) => String(c.id) === categoryId && c.type === next)) setCategoryId('');
  }

  async function save(e) {
    e.preventDefault();
    // friendly checks first; the backend validates the same rules again
    const problems = {};
    if (!(Number(amount) > 0)) problems.amount = 'Enter an amount above ₹0';
    if (!date) problems.date = 'Pick a date';
    if (!categoryId) problems.categoryId = 'Choose a category';
    if (Object.keys(problems).length) {
      setError(new ApiError(400, 'Validation failed', problems));
      return;
    }
    setBusy(true);
    setError(null);
    const body = {
      type,
      amount: Number(amount),
      date,
      categoryId: categoryId ? Number(categoryId) : null,
      description: description.trim() || null,
    };
    try {
      if (isEdit) await api.updateTransaction(tx.id, body);
      else await api.createTransaction(body);
      refresh();
      onClose();
    } catch (err) {
      setError(err);
      setBusy(false);
    }
  }

  async function remove() {
    setBusy(true);
    try {
      await api.deleteTransaction(tx.id);
      refresh();
      onClose();
    } catch (err) {
      setError(err);
      setBusy(false);
    }
  }

  return (
    <Modal title={isEdit ? 'Edit transaction' : 'Add transaction'} onClose={onClose}>
      <form onSubmit={save} className="stack" noValidate>
        <Segmented
          label="Type"
          value={type}
          onChange={changeType}
          options={[
            { value: 'EXPENSE', label: 'Money out' },
            { value: 'INCOME', label: 'Money in' },
          ]}
        />

        <div className="form-row">
          <Field label="Amount" error={fieldError('amount')} className="grow">
            <span className="input-affix">
              <span aria-hidden="true">₹</span>
              <input
                type="number"
                inputMode="decimal"
                min="0.01"
                step="0.01"
                required
                autoFocus
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                placeholder="0.00"
              />
            </span>
          </Field>
          <Field label="Date" error={fieldError('date')}>
            <input type="date" required value={date} onChange={(e) => setDate(e.target.value)} />
          </Field>
        </div>

        <Field label="Category" error={fieldError('categoryId')}>
          <select required value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
            <option value="" disabled>
              Choose a category
            </option>
            {options.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </Field>

        <Field label="Description" hint="Optional, e.g. “Dinner with Yash”" error={fieldError('description')}>
          <input type="text" maxLength={255} value={description} onChange={(e) => setDescription(e.target.value)} />
        </Field>

        <FormError error={error} />

        <div className="modal-actions">
          {isEdit &&
            (confirmDelete ? (
              <button type="button" className="btn btn-danger push-left" onClick={remove} disabled={busy}>
                Yes, delete it
              </button>
            ) : (
              <button type="button" className="btn btn-quiet-danger push-left" onClick={() => setConfirmDelete(true)}>
                Delete
              </button>
            ))}
          <button type="button" className="btn" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={busy}>
            {busy ? 'Saving…' : isEdit ? 'Save changes' : 'Add transaction'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
