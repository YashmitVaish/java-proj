-- Runs on every start. IF NOT EXISTS makes it safe to run again.
-- Amounts are stored as whole paise (INTEGER, 1250.50 INR -> 125050) so sums stay exact;
-- dates as TEXT 'YYYY-MM-DD' so they sort and compare correctly.

CREATE TABLE IF NOT EXISTS users (
    id       INTEGER PRIMARY KEY AUTOINCREMENT,
    name     TEXT NOT NULL,
    email    TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS categories (
    id      INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id),
    name    TEXT NOT NULL,
    type    TEXT NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    system  INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS transactions (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id      INTEGER NOT NULL REFERENCES users(id),
    category_id  INTEGER NOT NULL REFERENCES categories(id),
    type         TEXT NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    amount_paise INTEGER NOT NULL,
    date         TEXT NOT NULL,
    description  TEXT
);

CREATE TABLE IF NOT EXISTS budgets (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id      INTEGER NOT NULL REFERENCES users(id),
    category_id  INTEGER REFERENCES categories(id),  -- NULL = overall budget
    month        TEXT NOT NULL,                       -- '2026-11'
    amount_paise INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_categories_user ON categories(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_user_date ON transactions(user_id, date);
CREATE INDEX IF NOT EXISTS idx_budgets_user_month ON budgets(user_id, month);
