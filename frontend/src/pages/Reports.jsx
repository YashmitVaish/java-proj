import { useState } from 'react';
import { CategoryBars, MonthlyChart } from '../components/charts.jsx';
import { LoadError, Loading, Segmented, Stepper, Swatch } from '../components/ui.jsx';
import { api } from '../lib/api.js';
import { EXPENSE_COLOR, INCOME_COLOR } from '../lib/colors.js';
import { money, monthLabel, yearRange } from '../lib/format.js';
import { useLoad, useTitle } from '../lib/hooks.js';
import { useApp } from '../state.jsx';

export default function Reports() {
  useTitle('Reports');
  const { month, version, colorOf } = useApp();
  const [year, setYear] = useState(Number(month.slice(0, 4)));
  const [type, setType] = useState('EXPENSE');
  const { from, to } = yearRange(year);

  const main = useLoad(
    () => Promise.all([api.summary(from, to), api.monthly(year)]).then(([summary, monthly]) => ({ summary, monthly })),
    [year, version],
  );
  const cats = useLoad(() => api.byCategory(from, to, type), [year, type, version]);

  const hasData = main.data && (Number(main.data.summary.totalIncome) > 0 || Number(main.data.summary.totalExpense) > 0);

  return (
    <>
      <div className="page-head">
        <h1>Reports</h1>
        <Stepper
          label={String(year)}
          onPrev={() => setYear(year - 1)}
          onNext={() => setYear(year + 1)}
          prevLabel="Previous year"
          nextLabel="Next year"
        />
      </div>

      {main.error && <LoadError error={main.error} onRetry={main.reload} />}
      {!main.data && main.loading && <Loading />}

      {main.data && !hasData && <p className="empty-block">No transactions in {year}.</p>}

      {hasData && (
        <>
          <dl className="figures">
            <div>
              <dt>Money in</dt>
              <dd className="num">{money(main.data.summary.totalIncome, { whole: true })}</dd>
            </div>
            <div>
              <dt>Money out</dt>
              <dd className="num">{money(main.data.summary.totalExpense, { whole: true })}</dd>
            </div>
            <div>
              <dt>{Number(main.data.summary.balance) >= 0 ? 'Kept' : 'Overspent'}</dt>
              <dd className="num">{money(Math.abs(main.data.summary.balance), { whole: true })}</dd>
            </div>
          </dl>

          <section aria-labelledby="rep-monthly" className="report-block">
            <div className="section-head">
              <h2 id="rep-monthly">Month by month</h2>
              <ul className="key" aria-label="Legend">
                <li>
                  <Swatch color={INCOME_COLOR} /> Money in
                </li>
                <li>
                  <Swatch color={EXPENSE_COLOR} /> Money out
                </li>
              </ul>
            </div>
            <MonthlyChart data={main.data.monthly} />
            <details className="table-toggle">
              <summary>Show as a table</summary>
              <table className="ledger compact">
                <thead>
                  <tr>
                    <th scope="col">Month</th>
                    <th scope="col" className="right">
                      In
                    </th>
                    <th scope="col" className="right">
                      Out
                    </th>
                    <th scope="col" className="right">
                      Net
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {main.data.monthly.map((m) => (
                    <tr key={m.month}>
                      <th scope="row">{monthLabel(m.month, { withYear: false })}</th>
                      <td className="right num">{money(m.income, { whole: true })}</td>
                      <td className="right num">{money(m.expense, { whole: true })}</td>
                      <td className={`right num ${Number(m.net) < 0 ? 'is-negative' : ''}`}>
                        {money(m.net, { whole: true })}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </details>
          </section>

          <section aria-labelledby="rep-cats" className="report-block">
            <div className="section-head">
              <h2 id="rep-cats">By category, {year}</h2>
              <Segmented
                label="Show"
                value={type}
                onChange={setType}
                options={[
                  { value: 'EXPENSE', label: 'Money out' },
                  { value: 'INCOME', label: 'Money in' },
                ]}
              />
            </div>
            {cats.error && <LoadError error={cats.error} onRetry={cats.reload} />}
            {cats.data && cats.data.length === 0 && (
              <p className="empty">No {type === 'EXPENSE' ? 'spending' : 'income'} recorded in {year}.</p>
            )}
            {cats.data && cats.data.length > 0 && <CategoryBars rows={cats.data} colorFor={(name) => colorOf(name, type)} />}
          </section>
        </>
      )}
    </>
  );
}
