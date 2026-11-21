import { useState } from 'react';
import { Amount } from '../components/money.jsx';
import { ConfirmDialog, LoadError, Loading, MonthStepper, Segmented, Swatch } from '../components/ui.jsx';
import { api } from '../lib/api.js';
import { dateLabel, money, monthLabel, monthRange, weekdayLabel } from '../lib/format.js';
import { useLoad, useTitle } from '../lib/hooks.js';
import { useApp } from '../state.jsx';

export default function Transactions() {
  useTitle('Transactions');
  const { month, setMonth, version, refresh, categories, colorOf, openTransaction } = useApp();

  const [type, setType] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [allTime, setAllTime] = useState(false);
  const [deleting, setDeleting] = useState(null);
  const [exporting, setExporting] = useState(false);
  const [exportError, setExportError] = useState(null);

  const period = allTime ? {} : monthRange(month);
  const filters = { ...period, type, categoryId };

  const { data, error, loading, reload } = useLoad(() => api.transactions(filters), [
    month,
    allTime,
    type,
    categoryId,
    version,
  ]);

  const categoryOptions = categories.filter((c) => !type || c.type === type);

  function changeType(next) {
    setType(next);
    if (categoryId && next && !categories.some((c) => String(c.id) === categoryId && c.type === next)) setCategoryId('');
  }

  async function exportCsv() {
    setExporting(true);
    setExportError(null);
    try {
      await api.exportCsv({ ...period, type });
    } catch (e) {
      setExportError(e);
    } finally {
      setExporting(false);
    }
  }

  const rows = data || [];
  const totalIn = rows.filter((t) => t.type === 'INCOME').reduce((s, t) => s + Number(t.amount), 0);
  const totalOut = rows.filter((t) => t.type === 'EXPENSE').reduce((s, t) => s + Number(t.amount), 0);

  return (
    <>
      <div className="page-head">
        <h1>Transactions</h1>
        <MonthStepper month={month} setMonth={setMonth} disabled={allTime} />
      </div>

      <div className="toolbar" role="group" aria-label="Filters">
        <Segmented
          label="Type"
          value={type}
          onChange={changeType}
          options={[
            { value: '', label: 'All' },
            { value: 'EXPENSE', label: 'Money out' },
            { value: 'INCOME', label: 'Money in' },
          ]}
        />
        <label className="select-inline">
          <span className="sr-only">Category</span>
          <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
            <option value="">All categories</option>
            {categoryOptions.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
                {!type ? (c.type === 'INCOME' ? ' (in)' : ' (out)') : ''}
              </option>
            ))}
          </select>
        </label>
        <label className="check">
          <input type="checkbox" checked={allTime} onChange={(e) => setAllTime(e.target.checked)} />
          Show all months
        </label>
        <button type="button" className="btn push-left" onClick={exportCsv} disabled={exporting}>
          <svg width="14" height="14" viewBox="0 0 14 14" aria-hidden="true">
            <path d="M7 1.5v8M3.5 6.5L7 10l3.5-3.5M2 12.5h10" stroke="currentColor" strokeWidth="1.6" fill="none" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
          {exporting ? 'Exporting…' : 'Export CSV'}
        </button>
      </div>
      {exportError && <LoadError error={exportError} />}
      {categoryId && (
        <p className="fine-print">The CSV export covers every category for this period; the backend doesn’t filter exports by category.</p>
      )}

      {error && <LoadError error={error} onRetry={reload} />}
      {!data && loading && <Loading />}

      {data && rows.length === 0 && (
        <div className="empty-block">
          <p>
            {type || categoryId
              ? 'No transactions match these filters.'
              : allTime
                ? 'You haven’t added any transactions yet.'
                : `No transactions in ${monthLabel(month)}.`}
          </p>
          <button type="button" className="btn btn-primary" onClick={() => openTransaction()}>
            Add a transaction
          </button>
        </div>
      )}

      {rows.length > 0 && (
        <>
          <dl className="totals">
            <div>
              <dt>Transactions</dt>
              <dd className="num">{rows.length}</dd>
            </div>
            <div>
              <dt>Money in</dt>
              <dd className="num">{money(totalIn)}</dd>
            </div>
            <div>
              <dt>Money out</dt>
              <dd className="num">{money(totalOut)}</dd>
            </div>
          </dl>

          <table className="ledger">
            <thead>
              <tr>
                <th scope="col">Date</th>
                <th scope="col">Description</th>
                <th scope="col">Category</th>
                <th scope="col" className="right">
                  Amount
                </th>
                <th scope="col">
                  <span className="sr-only">Actions</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {rows.map((t, i) => {
                const firstOfDay = i === 0 || rows[i - 1].date !== t.date;
                return (
                  <tr key={t.id} className={firstOfDay ? 'day-start' : ''}>
                    <td className="col-date">
                      {firstOfDay ? (
                        <>
                          <span className="date-main">{dateLabel(t.date)}</span>
                          <span className="date-sub">{weekdayLabel(t.date)}</span>
                        </>
                      ) : (
                        <span className="sr-only">{dateLabel(t.date)}</span>
                      )}
                    </td>
                    <td className="col-desc">{t.description || <span className="muted">No description</span>}</td>
                    <td className="col-cat">
                      <Swatch color={colorOf(t.category.id, t.type)} />
                      {t.category.name}
                    </td>
                    <td className="col-amount right">
                      <Amount tx={t} />
                    </td>
                    <td className="col-actions">
                      <button type="button" className="btn-link" onClick={() => openTransaction(t)}>
                        Edit<span className="sr-only"> {t.description || t.category.name}</span>
                      </button>
                      <button type="button" className="btn-link danger" onClick={() => setDeleting(t)}>
                        Delete<span className="sr-only"> {t.description || t.category.name}</span>
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </>
      )}

      {deleting && (
        <ConfirmDialog
          title="Delete this transaction?"
          confirmLabel="Delete transaction"
          onClose={() => setDeleting(null)}
          onConfirm={async () => {
            await api.deleteTransaction(deleting.id);
            refresh();
          }}
        >
          <p>
            {deleting.description || deleting.category.name}, {money(deleting.amount)} on {dateLabel(deleting.date)}. This
            can’t be undone.
          </p>
        </ConfirmDialog>
      )}
    </>
  );
}
