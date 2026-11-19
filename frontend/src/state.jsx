import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, getToken, setToken, setUnauthorizedHandler } from './lib/api.js';
import { categoryColors } from './lib/colors.js';
import { currentMonth } from './lib/format.js';

// ---------------------------------------------------------------------------
// Auth: token + current user. The token lives in localStorage and lasts 24h.
// ---------------------------------------------------------------------------

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setTokenState] = useState(getToken);
  const [user, setUser] = useState(null);
  const [sessionExpired, setSessionExpired] = useState(false);
  const [loggedOut, setLoggedOut] = useState(false); // true after pressing "Log out"

  const signIn = useCallback(({ token, user }) => {
    setToken(token);
    setTokenState(token);
    setUser(user);
    setSessionExpired(false);
    setLoggedOut(false);
  }, []);

  const clear = useCallback(() => {
    setToken(null);
    setTokenState(null);
    setUser(null);
  }, []);

  const signOut = useCallback(() => {
    clear();
    setLoggedOut(true);
  }, [clear]);

  // Any 401 on an authenticated request logs out and sends the user to the login page.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      clear();
      setSessionExpired(true);
    });
  }, [clear]);

  // On reload we only have the token, so fetch the user it belongs to.
  useEffect(() => {
    if (token && !user) api.me().then(setUser).catch(() => {});
  }, [token, user]);

  const value = useMemo(
    () => ({ token, user, setUser, signIn, signOut, sessionExpired, loggedOut }),
    [token, user, signIn, signOut, sessionExpired, loggedOut],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);

// ---------------------------------------------------------------------------
// App state shared by the signed-in pages: the month being looked at, the
// user's categories, and a version number that bumps after every change so
// pages know to refetch.
// ---------------------------------------------------------------------------

const AppContext = createContext(null);

export function AppProvider({ children }) {
  const [month, setMonth] = useState(currentMonth);
  const [version, setVersion] = useState(0);
  const [categories, setCategories] = useState([]);
  const [editing, setEditing] = useState(null); // null = closed, {} = new, transaction = edit

  const refresh = useCallback(() => setVersion((v) => v + 1), []);

  useEffect(() => {
    api.categories().then(setCategories).catch(() => {});
  }, [version]);

  const colorOf = useMemo(() => categoryColors(categories), [categories]);

  const value = useMemo(
    () => ({
      month,
      setMonth,
      version,
      refresh,
      categories,
      colorOf,
      editing,
      openTransaction: (tx = {}) => setEditing(tx),
      closeTransaction: () => setEditing(null),
    }),
    [month, version, refresh, categories, colorOf, editing],
  );
  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}

export const useApp = () => useContext(AppContext);
