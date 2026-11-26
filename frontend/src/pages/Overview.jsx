import { Link } from 'react-router-dom';
import { CategoryLegend, Meter, SpendingStrip } from '../components/charts.jsx';
import { Amount, BudgetWords } from '../components/money.jsx';
import { sortBudgets } from '../lib/budgets.js';
import { LoadError, Loading, MonthStepper, Refreshing, Swatch } from '../components/ui.jsx';
import { api } from '../lib/api.js';
import { NEUTRAL } from '../lib/colors.js';
import { dateLabel, money, monthLabel, monthRange } from '../lib/format.js';
import { useLoad, useTitle } from '../lib/hooks.js';
import { useApp } from '../state.jsx';

export default function Overview() {
  useTitle('Overview');
  const { month, setMonth, version, colorOf, openTransaction } = useApp();
  const { from, to } = monthRange(month);

  const { data, error, loading, reload } = useLoad(
    () =>
      Promise.all([
        api.summary(from, to),
        api.byCategory(from, to, 'EXPENSE'),
        api.budgetStatus(month),
        api.transactions({ from, to }),
      ]).then(([summary, byCategory, budgets, transactions]) => ({ summary, byCategory, budgets, transactions })),
    [month, version],
  );

  return (
    <>
      <div className="page-head is-month">
        <h1 className="sr-only">Overview for {monthLabel(month)}</h1>
        <MonthStepper month={month} setMonth={setMonth} />
      </div>

      {error && <LoadError error={error} onRetry={reload} />}
      {!data && loading && <Loading />}
      {data && (
        <Refreshing busy={loading}>
          <OverviewBody data={data} month={month} colorOf={colorOf} openTransaction={openTransaction} />
        </Refreshing>
      )}
    </>
  );
}

function OverviewBody({ data, month, colorOf, openTransaction }) {
  const income = Number(data.summary.totalIncome);
  const expense = Number(data.summary.totalExpense);
  const balance = Number(data.summary.balance);
  const name = monthLabel(month, { withYear: false });

  if (data.transactions.length === 0) {
    return (
      <section className="empty-hero">
        <h2 className="headline">Nothing written down for {name} yet</h2>
        <p className="lede">Add what you earned and what you spent, and this page fills in.</p>
        <button type="button" className="btn btn-primary" onClick={() => openTransaction()}>
          Add a transaction
        </button>
      </section>
    );
  }

  // Merge grey (uncategorised / overflow) categories into one strip segment so the colours stay readable.
  const coloured = [];
  let other = null;
  for (const row of data.byCategory) {
    const color = colorOf(row.category, 'EXPENSE');
    const total = Number(row.total);
    if (color === NEUTRAL) {
      other = other ? { ...other, name: 'Other', total: other.total + total } : { name: row.category, total, color };
    } else coloured.push({ name: row.category, total, color });
  }
  const parts = other ? [...coloured, other] : coloured;

  return (
    <>
      <section className="hero" aria-label="This month at a glance">
        <h2 className="headline">
          {income > 0 ? (
            <>
              {money(expense, { whole: true })} spent of {money(income, { whole: true })}
            </>
          ) : (
            <>{money(expense, { whole: true })} spent</>
          )}
        </h2>
        <p className="lede">
          {income === 0 && expense > 0 && <>No income recorded for {name}. Add it to see what’s left.</>}
          {income > 0 && balance >= 0 && (
            <>
              You kept <strong>{money(balance, { whole: true })}</strong>, {Math.round((balance / income) * 100)}% of what came
              in.
            </>
          )}
          {income > 0 && balance < 0 && (
            <>
              You spent <strong>{money(-balance, { whole: true })}</strong> more than came in.
            </>
          )}
        </p>

        {expense > 0 || income > 0 ? <SpendingStrip income={income} expense={expense} parts={parts} /> : null}
        {parts.length > 0 && <CategoryLegend parts={parts} total={expense} leftover={income - expense} />}
      </section>

      <div className="split">
        <section aria-labelledby="ov-budgets">
          <div className="section-head">
            <h2 id="ov-budgets">Budgets</h2>
            <Link to="/budgets">Manage budgets</Link>
          </div>
          {data.budgets.length === 0 ? (
            <p className="empty">
              No budgets for {name}. <Link to="/budgets">Set a spending limit</Link> to see how close you are.
            </p>
          ) : (
            <ul className="budget-mini">
              {sortBudgets(data.budgets).map((b) => (
                <li key={b.budgetId}>
                  <div className="budget-mini-top">
                    <span className="budget-mini-name">
                      {b.category === 'Overall' ? 'All spending' : b.category}
                    </span>
                    <BudgetWords b={b} />
                  </div>
                  <Meter percent={b.percentUsed} label={`${b.category}: ${Math.round(b.percentUsed)}% of limit used`} />
                </li>
              ))}
            </ul>
          )}
        </section>

        <section aria-labelledby="ov-latest">
          <div className="section-head">
            <h2 id="ov-latest">Latest</h2>
            <Link to="/transactions">All transactions</Link>
          </div>
          <ul className="mini-ledger">
            {data.transactions.slice(0, 6).map((t) => (
              <li key={t.id}>
                <button type="button" className="mini-ledger-row" onClick={() => openTransaction(t)}>
                  <span className="mini-date">{dateLabel(t.date)}</span>
                  <span className="mini-what">
                    <span className="mini-desc">{t.description || t.category.name}</span>
                    <span className="mini-cat">
                      <Swatch color={colorOf(t.category.id, t.type)} />
                      {t.category.name}
                    </span>
                  </span>
                  <Amount tx={t} />
                </button>
              </li>
            ))}
          </ul>
        </section>
      </div>
    </>
  );
}
