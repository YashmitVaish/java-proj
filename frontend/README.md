# Khata: expense tracker frontend

React + Vite frontend for the Java API in the parent folder. It covers every endpoint in `PLAN.md`
section 4: auth, profile and password, categories, transactions, monthly budgets, reports and CSV export.

## Run it

Needs Node 20 or newer. Start the backend first (`./mvnw package && java -jar target/expense-tracker.jar` in the parent folder), then:

```bash
cd frontend
npm install
npm run dev        # http://localhost:3000
```

The dev server runs on port **3000** because that's the origin the backend's CORS setting allows by default.
If you change the port, add it to `CORS_ALLOWED_ORIGINS` in the backend's `.env`.

The API address defaults to `http://localhost:8080/api`. To point somewhere else, copy `.env.example` to
`.env.local` and set `VITE_API_URL`.

```bash
npm run build      # production build in dist/
npm run preview    # serve that build on :3000
```

## Pages

| Page | What it does | Endpoints |
|---|---|---|
| Log in / Create account | Email + password; the JWT is kept in localStorage | `POST /auth/login`, `POST /auth/register` |
| Overview | The month at a glance: spent vs earned, a strip of spending by category, budget progress, latest transactions | `reports/summary`, `reports/by-category`, `budgets/status`, `transactions` |
| Transactions | Ledger for a month (or all months), filter by type and category, add / edit / delete, export CSV | `transactions` CRUD, `export/transactions.csv` |
| Budgets | Overall and per-category limits for a month, change or delete them, copy last month's budgets in one click | `budgets` CRUD, `budgets/status` |
| Reports | Year totals, income vs expense per month (with a table view), totals by category | `reports/summary`, `reports/monthly`, `reports/by-category` |
| Categories | Add, rename, delete. Uncategorized is locked, as the backend requires | `categories` CRUD |
| Settings | Change name or password, log out | `users/me`, `users/me/password` |

The month you pick with the ‹ › arrows is shared by Overview, Transactions and Budgets.

## How it talks to the API

All requests go through `src/lib/api.js`:

- sends `Authorization: Bearer <token>` on everything except `/auth/**`
- a 401 on an authenticated request (the 24h token expired) logs you out and shows a notice on the login page
- error bodies `{status, message, errors}` become an `ApiError`; field errors appear under the matching inputs
- CSV export is fetched with the token and saved as a file, since a plain link can't send the header

## Code layout

```
src/
├── main.jsx, App.jsx        routes, auth guard
├── state.jsx                AuthProvider (token, user) and AppProvider (month, categories, refresh)
├── lib/
│   ├── api.js               fetch wrapper + one function per endpoint
│   ├── format.js            ₹ formatting with Indian digit grouping, date/month helpers
│   ├── colors.js            category colours
│   ├── budgets.js, hooks.js, tooltip.jsx
├── components/              Layout, TransactionForm (modal), charts, shared UI
├── pages/                   one file per page
└── styles.css               all styling, no CSS framework
```

## Design notes

- Type is Anek Latin (by Ek Type), bundled through `@fontsource`, so it works offline. It's one variable family:
  condensed widths for figures and headings, normal width for text.
- Category colours come from Indian banknotes (₹50 blue, ₹10 brown, ₹100 lavender, ₹200 yellow, ₹2000 magenta,
  ₹20 green) and were checked for colour-blind separation. A category keeps its colour on every page.
  Categories past the seventh, and Uncategorized, are grey.
- Amounts use `en-IN` formatting, so ₹1,23,456 and ₹1.2L on chart axes.
- The app name "Khata" is only display text (`index.html`, `Layout.jsx`, `AuthPage.jsx`, `hooks.js`), so it's easy to rename.
