# expense tracker backend

Spring Boot / Java 25 REST API for an expense tracker. Postgres on Supabase, JWT login.
Full design + every endpoint: [PLAN.md](PLAN.md).

## run locally

Needs JDK 25. Maven isn't needed because the wrapper (`./mvnw`) downloads it.

```bash
cp .env.example .env      # fill in DB_URL / DB_USERNAME / DB_PASSWORD from supabase + a JWT_SECRET
./mvnw spring-boot:run    # http://localhost:8080
```

Tables get created automatically on first start.

Generate a JWT secret: `openssl rand -base64 64 | tr -d '\n'`

## for the frontend

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
