# Expense Tracker — Backend Plan

Backend only. The frontend is built separately and talks to this API over JSON/HTTP.

## 1. Decisions

| Area | Decision |
|---|---|
| Language / framework | Java 25, Spring Boot 4.x |
| Build | Maven (`./mvnw` wrapper) |
| Database | PostgreSQL on Supabase. Tables are created by Hibernate (`ddl-auto: update`) |
| Auth | Register/login with email + password (BCrypt). Login returns a JWT that's valid for 24h |
| Currency | INR only |
| Categories | Each user has their own. New users get a starter set plus "Uncategorized" (one for income, one for expense) |
| Deleting a category | Its transactions move to "Uncategorized"; budgets for it are deleted |
| Features | Income + expenses, categories, monthly budgets, reports, CSV export |
| CORS | Allowed frontend origin(s) come from `.env`, default `http://localhost:3000` |
| Run | Locally, for a demo |

## 2. Folder structure

```
src/main/java/com/example/expensetracker/
├── ExpenseTrackerApplication.java
├── config/       SecurityConfig          (which URLs need login, CORS, password encoder)
├── security/     JwtUtil, JwtFilter      (create + check tokens)
├── model/        User, Category, Transaction, Budget, TransactionType   (database tables)
├── repository/   one Spring Data repository per model
├── dto/          request/response classes (records)
├── service/      business logic
├── controller/   REST endpoints
└── exception/    ApiException + GlobalExceptionHandler
src/main/resources/application.yml
```

## 3. Tables (created automatically from the model classes)

- **users**: id, name, email (unique), password (BCrypt hash)
- **categories**: id, user_id, name, type (INCOME/EXPENSE), system (true only for "Uncategorized")
- **transactions**: id, user_id, category_id, type, amount (numeric 12,2), date, description
- **budgets**: id, user_id, category_id (null = overall budget), month ("2026-11"), amount

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

## 6. Config (`.env`)

| Variable | Default |
|---|---|
| `DB_URL` | none: `jdbc:postgresql://<supabase-host>:5432/postgres?sslmode=require` |
| `DB_USERNAME` / `DB_PASSWORD` | none: from Supabase |
| `JWT_SECRET` | none: at least 32 chars |
| `JWT_EXPIRATION_HOURS` | `24` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` |
| `SERVER_PORT` | `8080` |

Use Supabase's **direct connection or session pooler (port 5432)**, not the 6543 transaction pooler.

## 7. Build order

1. Spring Boot setup + config
2. Register/login with JWT, `/me`
3. Categories (+ starter set on register)
4. Transactions (+ move to Uncategorized on category delete)
5. Budgets + status
6. Reports
7. CSV export
