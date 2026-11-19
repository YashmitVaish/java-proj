import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import Layout from './components/Layout.jsx';
import AuthPage from './pages/AuthPage.jsx';
import Budgets from './pages/Budgets.jsx';
import Categories from './pages/Categories.jsx';
import Overview from './pages/Overview.jsx';
import Reports from './pages/Reports.jsx';
import Settings from './pages/Settings.jsx';
import Transactions from './pages/Transactions.jsx';
import { AppProvider, AuthProvider, useAuth } from './state.jsx';

function RequireAuth({ children }) {
  const { token, loggedOut } = useAuth();
  const location = useLocation();
  // remember where the user was (for expired sessions and bookmarks), but not after "Log out"
  if (!token) return <Navigate to="/login" replace state={loggedOut ? undefined : { from: location.pathname }} />;
  return <AppProvider>{children}</AppProvider>;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<AuthPage mode="login" />} />
          <Route path="/register" element={<AuthPage mode="register" />} />
          <Route
            element={
              <RequireAuth>
                <Layout />
              </RequireAuth>
            }
          >
            <Route index element={<Overview />} />
            <Route path="transactions" element={<Transactions />} />
            <Route path="budgets" element={<Budgets />} />
            <Route path="reports" element={<Reports />} />
            <Route path="categories" element={<Categories />} />
            <Route path="settings" element={<Settings />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
