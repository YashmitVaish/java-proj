# Expense Tracker — Backend Plan

Backend only. The frontend is built separately and talks to this API over JSON/HTTP.

## 1. Decisions

| Area | Decision |
|---|---|
| Language | Java 25, plain Java SE (no framework) |
| Build | Maven (`./mvnw` wrapper), shade plugin builds one runnable jar |
| HTTP | JDK built-in `com.sun.net.httpserver.HttpServer` + a small hand-written `Router` |
| Database | SQLite file (`khata.db`) through plain JDBC (`PreparedStatement` / `ResultSet`). Tables come from `schema.sql` on startup |
| Data access | DAO pattern: one interface per table (`dao/`) + a JDBC implementation (`dao/jdbc/`) |
| Auth | Register/login with email + password (BCrypt via jbcrypt). Login returns a JWT that's valid for 24h |
| JSON | Jackson |
| Logging / config | `java.util.logging` (`logging.properties`), settings in `application.properties` + `.env` |
| Currency | INR only, stored as whole paise (INTEGER) so totals are exact |
| Categories | Each user has their own. New users get a starter set plus "Uncategorized" (one for income, one for expense) |
| Deleting a category | Its transactions move to "Uncategorized"; budgets for it are deleted (one DB transaction) |
| Features | Income + expenses, categories, monthly budgets, reports, CSV export |
| CORS | Allowed frontend origin(s) come from `.env`, default `http://localhost:3000` |
| Run | Locally, for a demo |

## 2. Folder structure

```
src/main/java/com/example/expensetracker/
├── Main.java          starts everything: config -> DB -> DAOs -> services -> controllers -> HttpServer
├── config/            AppConfig                (application.properties + .env + env vars)
├── db/                Database, DataAccessException   (connections, schema, inTransaction)
├── dao/               UserDao, CategoryDao, TransactionDao, BudgetDao   (interfaces)
├── dao/jdbc/          BaseDao (abstract) + JdbcUserDao, JdbcCategoryDao, JdbcTransactionDao, JdbcBudgetDao
├── model/             User, Category, Transaction, Budget, TransactionType
├── dto/               request/response records (requests check themselves in validate())
├── validation/        Validatable, Validator
├── service/           business logic
├── controller/        register each endpoint on the Router
├── http/              Router, Request, Response, Handler, Cors, Json
├── security/          JwtUtil, PasswordHasher
└── exception/         ApiException, ValidationException
src/main/resources/    application.properties, schema.sql, logging.properties
```

## 3. Tables (`schema.sql`)

- **users**: id, name, email (unique), password (BCrypt hash)
- **categories**: id, user_id, name, type (INCOME/EXPENSE), system (1 only for "Uncategorized")
- **transactions**: id, user_id, category_id, type, amount_paise, date (`YYYY-MM-DD`), description
- **budgets**: id, user_id, category_id (NULL = overall budget), month ("2026-11"), amount_paise

Starter categories:
- **Expense:** Food, Rent, Transport, Shopping, Bills & Utilities, Health, Entertainment, Uncategorized
- **Income:** Salary, Freelance, Gifts, Uncategorized

## 4. API

Everything except `/api/auth/**` needs the header `Authorization: Bearer <token>`.
Users only ever see their own data; someone else's id gives 404.

### Auth
| Method | Path | Body | Response |
|---|---|---|---|
| POST | `/api/auth/register` | `{name, email, password}` | 201 `{token, user}` |
| POST | `/api/auth/login` | `{email, password}` | 200 `{token, user}` / 401 |

### User
| Method | Path | Body |
|---|---|---|
| GET | `/api/users/me` | none |
| PUT | `/api/users/me` | `{name}` |
| PUT | `/api/users/me/password` | `{currentPassword, newPassword}` → 204 |

### Categories
| Method | Path | Notes |
|---|---|---|
| GET | `/api/categories?type=EXPENSE` | `type` optional |
| POST | `/api/categories` | `{name, type}`; 409 if it already exists |
| PUT | `/api/categories/{id}` | `{name}` (rename) |
| DELETE | `/api/categories/{id}` | transactions move to Uncategorized |

### Transactions
| Method | Path | Notes |
|---|---|---|
| GET | `/api/transactions?type&from&to&categoryId` | all filters optional; newest first |
| GET | `/api/transactions/{id}` | |
| POST | `/api/transactions` | `{type, amount, date, categoryId, description}` |
| PUT | `/api/transactions/{id}` | same body |
| DELETE | `/api/transactions/{id}` | |

Example transaction:
```json
{ "id": 12, "userId": 1, "type": "EXPENSE", "amount": 450.00, "date": "2026-11-15",
  "description": "Dinner", "category": { "id": 3, "userId": 1, "name": "Food", "type": "EXPENSE", "system": false } }
```

### Budgets (expense categories only)
| Method | Path | Notes |
|---|---|---|
| GET | `/api/budgets?month=2026-11` | |
| POST | `/api/budgets` | `{categoryId or null, month, amount}`; 409 if one already exists |
| PUT | `/api/budgets/{id}` | same body, only `amount` changes |
| DELETE | `/api/budgets/{id}` | |
| GET | `/api/budgets/status?month=2026-11` | `[{budgetId, month, category, limit, spent, remaining, percentUsed, exceeded}]` |

### Reports
| Method | Path | Response |
|---|---|---|
| GET | `/api/reports/summary?from&to` | `{totalIncome, totalExpense, balance}` |
| GET | `/api/reports/by-category?from&to&type=EXPENSE` | `[{category, total, percentage}]` |
| GET | `/api/reports/monthly?year=2026` | 12 × `{month, income, expense, net}` |

### Export
| Method | Path | Notes |
|---|---|---|
| GET | `/api/export/transactions.csv?from&to&type` | downloads a CSV: date, type, category, amount_inr, description |

## 5. Errors

```json
{ "status": 404, "message": "Category not found" }
{ "status": 400, "message": "Validation failed", "errors": { "amount": "must be greater than 0" } }
```
400 = bad input, 401 = no/invalid token or wrong login, 404 = not found, 409 = duplicate.

## 6. Config

Looked up in this order: environment variable, then `.env`, then `application.properties`.

| Variable | Default |
|---|---|
| `JWT_SECRET` | none: required, at least 32 chars |
| `DB_PATH` | `khata.db` |
| `JWT_EXPIRATION_HOURS` | `24` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` |
| `SERVER_PORT` | `8080` |

Every CSV export also saves a copy on the server in `exports/`.
