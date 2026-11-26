# expense tracker backend

Plain Java 25 REST API for an expense tracker. No framework: JDK `HttpServer`, JDBC + DAOs on SQLite, JWT login.
Full design + every endpoint: [PLAN.md](PLAN.md).

## run locally

Needs JDK 25. Maven isn't needed because the wrapper (`./mvnw`) downloads it.

```bash
cp .env.example .env      # put a JWT_SECRET in it
./mvnw package            # builds target/expense-tracker.jar
java -jar target/expense-tracker.jar    # http://localhost:8080
```

Or without building a jar: `./mvnw compile exec:java`.

The database is a single file, `khata.db`, created with all its tables on first start.
Delete it to start fresh. Generate a JWT secret: `openssl rand -base64 64 | tr -d '\n'`

## frontend

React + Vite app in [`frontend/`](frontend/README.md). Start the backend, then:

```bash
cd frontend
npm install
npm run dev        # http://localhost:3000
```

## api notes for the frontend

- base url: `http://localhost:8080/api`
- CORS allows `http://localhost:3000` by default (change `CORS_ALLOWED_ORIGINS` in `.env`)
- register/login return `{token, user}`. Send `Authorization: Bearer <token>` on every other request.
  The token lasts 24h; on a 401 send the user back to login.
- dates `YYYY-MM-DD`, months `YYYY-MM`, amounts are numbers in INR
- errors look like `{"status": 400, "message": "...", "errors": {"field": "msg"}}`
- endpoint list: PLAN.md section 4

### quick test

```bash
curl -s -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"name":"Yash","email":"yash@test.com","password":"password123"}'
# copy the token from the response, then
curl -s localhost:8080/api/categories -H "Authorization: Bearer <token>"
```

## syllabus map (UCS553)

Where each unit shows up in the code:

| Unit | Topic | Where |
|---|---|---|
| 1 | packages, classes, interfaces, abstract classes | layered packages; `dao/*Dao` interfaces; abstract `dao/jdbc/BaseDao` extended by every `Jdbc*Dao` |
| 1 | static vs instance, access modifiers | `ApiException.notFound(...)` factories, `Json.MAPPER`, private fields + getters in `model/` |
| 1 | Maven | `pom.xml`: dependencies, exec plugin, shade plugin for the runnable jar |
| 2 | custom exceptions, hierarchy | `ApiException` -> `ValidationException`; `DataAccessException` wraps `SQLException`; `Router.toErrorResponse` |
| 2 | collections, generics, comparators | `HashMap`/`EnumMap`/`TreeMap` in `BudgetService`, `ReportService`, `JdbcTransactionDao`; generic `RowMapper<T>`, `queryList<T>`; `Comparator.comparing` in `ExportService` |
| 2 | Optional, LocalDate/YearMonth | every `findByIdAndUser` returns `Optional`; `YearMonth` in `BudgetService`, `ReportService` |
| 3 | file I/O | `ExportService` (`BufferedWriter` saves a copy to `exports/`), `Database.initSchema` reads `schema.sql`, `AppConfig` reads `.env` |
| 3 | logging | `java.util.logging` everywhere, configured by `logging.properties`; every request is logged by `Router` |
| 3 | properties files | `application.properties` loaded with `java.util.Properties` in `AppConfig` |
| 4 | JDBC, PreparedStatement, ResultSet | `BaseDao` (connect, bind `?`, read rows), all `Jdbc*Dao` classes |
| 4 | INSERT / SELECT / UPDATE / DELETE | `JdbcTransactionDao` has all four, plus `SUM ... GROUP BY` report queries and a dynamic `WHERE` for filters |
| 4 | transaction management | `Database.inTransaction` (`setAutoCommit(false)`, `commit`, `rollback`), used by `AuthService.register` and `CategoryService.delete` |
| 4 | DAO pattern | `dao/` interfaces, `dao/jdbc/` implementations, services only see the interfaces |
