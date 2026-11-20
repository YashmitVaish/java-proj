import { money } from '../lib/format.js';
import { budgetState } from '../lib/budgets.js';

export function BudgetWords({ b }) {
  const state = budgetState(b.percentUsed);
  if (state === 'over')
    return (
      <span className="budget-words is-over">
        <WarnIcon /> {money(-b.remaining, { whole: true })} over
      </span>
    );
  return (
    <span className={`budget-words ${state === 'near' ? 'is-near' : ''}`}>
      {state === 'near' && <WarnIcon />}
      {money(b.remaining, { whole: true })} left of {money(b.limit, { whole: true })}
    </span>
  );
}

function WarnIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 14 14" aria-hidden="true" className="warn-icon">
      <path d="M7 1.5L13 12.5H1Z" fill="currentColor" />
      <path d="M7 5.5v3.2M7 10.4v.1" stroke="#fff" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  );
}

export function Amount({ tx }) {
  const income = tx.type === 'INCOME';
  return (
    <span className={`amount num ${income ? 'is-in' : 'is-out'}`}>
      {income ? '+' : '−'}
      {money(tx.amount)}
    </span>
  );
}
