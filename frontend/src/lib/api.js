// Thin wrapper around fetch for the backend API (see PLAN.md section 4 in the backend).

const BASE = (import.meta.env.VITE_API_URL || 'http://localhost:8080/api').replace(/\/+$/, '');
const TOKEN_KEY = 'khata.token';

export class ApiError extends Error {
  constructor(status, message, fieldErrors = {}) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors || {};
  }
}

// --- token storage -------------------------------------------------------

export function getToken() {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setToken(token) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  } catch {
    // storage can be unavailable (private mode); the session then lasts until reload
  }
}

// Called when a request made *with* a token comes back 401 (expired or invalid token).
let onUnauthorized = () => {};
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn;
}

// --- core request --------------------------------------------------------

async function request(path, { method = 'GET', body, query, auth = true, raw = false } = {}) {
  const url = new URL(BASE + path, window.location.origin);
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== '') url.searchParams.set(key, value);
    }
  }

  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const token = auth ? getToken() : null;
  if (token) headers.Authorization = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(url, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  } catch {
    throw new ApiError(0, `Can't reach the server at ${BASE}. Check that the backend is running.`);
  }

  if (res.status === 401 && token) {
    onUnauthorized();
    throw new ApiError(401, 'Your session has expired. Log in again.');
  }

  if (!res.ok) {
    const data = await res.json().catch(() => null);
    throw new ApiError(res.status, data?.message || `Request failed with status ${res.status}`, data?.errors);
  }

  if (raw) return res;
  if (res.status === 204) return null;
  return res.json();
}

// --- endpoints -----------------------------------------------------------

export const api = {
  // auth
  register: (data) => request('/auth/register', { method: 'POST', body: data, auth: false }),
  login: (data) => request('/auth/login', { method: 'POST', body: data, auth: false }),

  // user
  me: () => request('/users/me'),
  updateMe: (data) => request('/users/me', { method: 'PUT', body: data }),
  changePassword: (data) => request('/users/me/password', { method: 'PUT', body: data }),

  // categories
  categories: (type) => request('/categories', { query: { type } }),
  createCategory: (data) => request('/categories', { method: 'POST', body: data }),
  renameCategory: (id, name) => request(`/categories/${id}`, { method: 'PUT', body: { name } }),
  deleteCategory: (id) => request(`/categories/${id}`, { method: 'DELETE' }),

  // transactions: filters = { type, from, to, categoryId }
  transactions: (filters) => request('/transactions', { query: filters }),
  createTransaction: (data) => request('/transactions', { method: 'POST', body: data }),
  updateTransaction: (id, data) => request(`/transactions/${id}`, { method: 'PUT', body: data }),
  deleteTransaction: (id) => request(`/transactions/${id}`, { method: 'DELETE' }),

  // budgets
  budgets: (month) => request('/budgets', { query: { month } }),
  budgetStatus: (month) => request('/budgets/status', { query: { month } }),
  createBudget: (data) => request('/budgets', { method: 'POST', body: data }),
  updateBudget: (id, data) => request(`/budgets/${id}`, { method: 'PUT', body: data }),
  deleteBudget: (id) => request(`/budgets/${id}`, { method: 'DELETE' }),

  // reports
  summary: (from, to) => request('/reports/summary', { query: { from, to } }),
  byCategory: (from, to, type = 'EXPENSE') => request('/reports/by-category', { query: { from, to, type } }),
  monthly: (year) => request('/reports/monthly', { query: { year } }),

  // export: downloads a CSV through the browser (needs the auth header, so a plain link won't do)
  async exportCsv(filters) {
    const res = await request('/export/transactions.csv', { query: filters, raw: true });
    const blob = await res.blob();
    const disposition = res.headers.get('Content-Disposition') || '';
    const name = disposition.match(/filename="?([^"]+)"?/)?.[1] || 'transactions.csv';
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = name;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(link.href), 1000);
  },
};
