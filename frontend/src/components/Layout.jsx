import { NavLink, Outlet } from 'react-router-dom';
import { useApp, useAuth } from '../state.jsx';
import TransactionForm from './TransactionForm.jsx';

const NAV = [
  { to: '/', label: 'Overview', end: true },
  { to: '/transactions', label: 'Transactions' },
  { to: '/budgets', label: 'Budgets' },
  { to: '/reports', label: 'Reports' },
  { to: '/categories', label: 'Categories' },
];

export default function Layout() {
  const { user } = useAuth();
  const { editing, openTransaction, closeTransaction } = useApp();

  return (
    <div className="shell">
      <a href="#main" className="skip-link">
        Skip to content
      </a>
      <header className="topbar">
        <div className="topbar-inner">
          <NavLink to="/" className="wordmark" aria-label="Khata, go to overview">
            Khata
          </NavLink>
          <nav className="nav" aria-label="Main">
            {NAV.map((item) => (
              <NavLink key={item.to} to={item.to} end={item.end}>
                {item.label}
              </NavLink>
            ))}
          </nav>
          <div className="topbar-actions">
            <button type="button" className="btn btn-primary" onClick={() => openTransaction()}>
              <svg width="14" height="14" viewBox="0 0 14 14" aria-hidden="true">
                <path d="M7 1.5v11M1.5 7h11" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              </svg>
              <span>Add transaction</span>
            </button>
            <NavLink to="/settings" className="user-link" title="Settings">
              <span className="avatar" aria-hidden="true">
                {(user?.name || '?').trim().charAt(0).toUpperCase()}
              </span>
              <span className="user-name">{user?.name || 'Settings'}</span>
            </NavLink>
          </div>
        </div>
      </header>

      <main id="main" className="page">
        <Outlet />
      </main>

      {editing && <TransactionForm key={editing.id || 'new'} tx={editing} onClose={closeTransaction} />}
    </div>
  );
}
