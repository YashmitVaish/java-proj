import { useState } from 'react';
import { Meter } from '../components/charts.jsx';
import { BudgetWords } from '../components/money.jsx';
import { sortBudgets } from '../lib/budgets.js';
import { ConfirmDialog, Field, FormError, LoadError, Loading, MonthStepper, Refreshing, Swatch } from '../components/ui.jsx';
import { api, ApiError } from '../lib/api.js';
import { money, monthLabel, shiftMonth } from '../lib/format.js';
import { useLoad, useTitle } from '../lib/hooks.js';
import { useApp } from '../state.jsx';

const OVERALL = 'overall';

export default function Budgets() {
  useTitle('Budgets');
  const { month, setMonth, version, refresh, categories, colorOf } = useApp();
  const prevMonth = shiftMonth(month, -1);

  const { data, error, loading, reload } = useLoad(
    () =>
      Promise.all([api.budgets(month), api.budgetStatus(month), api.budgets(prevMonth)]).then(
        ([budgets, status, previous]) => ({ budgets, status, previous }),
      ),
    [month, version],
  );

  const [deleting, setDeleting] = useState(null);
  const [copying, setCopying] = useState(false);
  const [copyError, setCopyError] = useState(null);

  const expenseCats = categories.filter((c) => c.type === 'EXPENSE');
  const budgetById = new Map((data?.budgets || []).map((b) => [b.id, b]));
  const taken = new Set((data?.budgets || []).map((b) => (b.category ? b.category.id : OVERALL)));

  async function copyPrevious() {
    setCopying(true);
    setCopyError(null);
    try {
      for (const b of data.previous) {
        await api.createBudget({ categoryId: b.category ? b.category.id : null, month, amount: b.amount });
      }
      refresh();
    } catch (e) {
      setCopyError(e);
      refresh();
    } finally {
      setCopying(false);
    }
  }

  return (
    <>
      <div className="page-head">
        <h1>Budgets</h1>
        <MonthStepper month={month} setMonth={setMonth} />
      </div>
      <p className="page-intro">
        Set a spending limit for the whole month, or for single expense categories. Limits are per month.
      </p>

      {error && <LoadError error={error} onRetry={reload} />}
      {!data && loading && <Loading />}

      <Refreshing busy={loading && !!data}>

      {data && (
        <>
          {data.status.length === 0 ? (
            <div className="empty-block">
              <p>No budgets for {monthLabel(month)} yet.</p>
              {data.previous.length > 0 && (
                <button type="button" className="btn" onClick={copyPrevious} disabled={copying}>
                  {copying
                    ? 'Copying…'
                    : `Copy ${data.previous.length} ${data.previous.length === 1 ? 'budget' : 'budgets'} from ${monthLabel(prevMonth, { withYear: false })}`}
                </button>
              )}
              <FormError error={copyError} />
            </div>
          ) : (
            <ul className="budget-list">
              {sortBudgets(data.status).map((s) => {
                const budget = budgetById.get(s.budgetId);
                const color = budget?.category ? colorOf(budget.category.id, 'EXPENSE') : null;
                return (
                  <BudgetRow
                    key={s.budgetId}
                    status={s}
                    budget={budget}
                    color={color}
                    onDelete={() => setDeleting(s)}
                    onSaved={refresh}
                  />
                );
              })}
            </ul>
          )}

          <AddBudget month={month} categories={expenseCats} taken={taken} onAdded={refresh} />
        </>
      )}

      </Refreshing>

      {deleting && (
        <ConfirmDialog
          title="Delete this budget?"
          confirmLabel="Delete budget"
          onClose={() => setDeleting(null)}
          onConfirm={async () => {
            await api.deleteBudget(deleting.budgetId);
            refresh();
          }}
        >
          <p>
            The {money(deleting.limit, { whole: true })} limit for{' '}
            {deleting.category === 'Overall' ? 'all spending' : deleting.category} in {monthLabel(month)} will be removed.
            Your transactions stay as they are.
          </p>
        </ConfirmDialog>
      )}
    </>
  );
}

function BudgetRow({ status: s, budget, color, onDelete, onSaved }) {
  const [editing, setEditing] = useState(false);
  const [amount, setAmount] = useState(String(s.limit));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const isOverall = s.category === 'Overall';
  const label = isOverall ? 'All spending' : s.category;

  async function save(e) {
    e.preventDefault();
    if (!(Number(amount) > 0)) {
      setError(new ApiError(400, 'Validation failed', { amount: 'Enter a limit above ₹0' }));
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await api.updateBudget(s.budgetId, { categoryId: budget?.category?.id ?? null, month: s.month, amount: Number(amount) });
      setEditing(false);
      onSaved();
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }

  return (
    <li className={`budget-row ${isOverall ? 'is-overall' : ''}`}>
      <div className="budget-row-head">
        <span className="budget-name">
          {color && <Swatch color={color} />}
          {label}
        </span>
        <BudgetWords b={s} />
      </div>
      <Meter percent={s.percentUsed} label={`${label}: ${Math.round(s.percentUsed)}% of limit used`} />
      <div className="budget-row-foot">
        <span className="muted num">
          {money(s.spent)} spent, {Math.round(s.percentUsed)}% of {money(s.limit)}
        </span>
        {!editing && (
          <span className="row-actions">
            <button type="button" className="btn-link" onClick={() => setEditing(true)}>
              Change limit<span className="sr-only"> for {label}</span>
            </button>
            <button type="button" className="btn-link danger" onClick={onDelete}>
              Delete<span className="sr-only"> budget for {label}</span>
            </button>
          </span>
        )}
      </div>
      {editing && (
        <form className="inline-form" onSubmit={save} noValidate>
          <Field label={`New limit for ${label}`} error={error?.fieldErrors?.amount}>
            <span className="input-affix">
              <span aria-hidden="true">₹</span>
              <input
                type="number"
                inputMode="decimal"
                min="1"
                step="0.01"
                autoFocus
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
              />
            </span>
          </Field>
          <button type="submit" className="btn btn-primary" disabled={busy}>
            {busy ? 'Saving…' : 'Save limit'}
          </button>
          <button
            type="button"
            className="btn"
            onClick={() => {
              setEditing(false);
              setAmount(String(s.limit));
              setError(null);
            }}
          >
            Cancel
          </button>
          <FormError error={error} />
        </form>
      )}
    </li>
  );
}

function AddBudget({ month, categories, taken, onAdded }) {
  const available = categories.filter((c) => !taken.has(c.id));
  const overallFree = !taken.has(OVERALL);
  const [target, setTarget] = useState('');
  const [amount, setAmount] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  if (!overallFree && available.length === 0) return null;

  async function add(e) {
    e.preventDefault();
    if (!(Number(amount) > 0)) {
      setError(new ApiError(400, 'Validation failed', { amount: 'Enter a limit above ₹0' }));
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await api.createBudget({ categoryId: target === OVERALL ? null : Number(target), month, amount: Number(amount) });
      setTarget('');
      setAmount('');
      onAdded();
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="add-section" aria-labelledby="add-budget">
      <h2 id="add-budget">Add a budget for {monthLabel(month)}</h2>
      <form className="inline-form" onSubmit={add} noValidate>
        <Field label="Limit for" error={error?.fieldErrors?.categoryId}>
          <select required value={target} onChange={(e) => setTarget(e.target.value)}>
            <option value="" disabled>
              Choose what to limit
            </option>
            {overallFree && <option value={OVERALL}>All spending this month</option>}
            {available.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Amount" error={error?.fieldErrors?.amount}>
          <span className="input-affix">
            <span aria-hidden="true">₹</span>
            <input
              type="number"
              inputMode="decimal"
              min="1"
              step="0.01"
              placeholder="5000"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
            />
          </span>
        </Field>
        <button type="submit" className="btn btn-primary" disabled={busy || !target}>
          {busy ? 'Adding…' : 'Add budget'}
        </button>
        <FormError error={error} />
      </form>
    </section>
  );
}
