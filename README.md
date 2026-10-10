# Granary

Granary is a community recipe box. People write recipes with structured ingredients, steps, photos and hashtag-style
tags, then find, bookmark, share and discuss each other's recipes.

This README is for developers working on the codebase: what the app does, how it is built, and how to run, test and
deploy it.

## Features

- **Recipes**: title, description, cooking time, ingredient lines (picked from a catalogue of ingredient suggestions,
  with quantity, unit and an "optional" flag), ordered steps, and up to 3 photos per recipe.
- **Tags**: hashtag-style tags (`#vegan`, `#quick`), stored lowercase without the `#`. The app ships with 122 default
  food tags in categories (meal, diet, protein, cuisine…). Users can also create their own, and `#hashtags` written in a
  title or description are applied automatically. Each recipe can have at most 10 tags.
- **Search**: every word must match the title, description, a tag or an ingredient. Results are ranked by where the
  words matched (title above tags and ingredients, which rank above the description) and paged.
- **Browsing**: home feed ordered by bookmarks, trending recipes and tags, and a page per tag (`/tag/vegan`).
- **People**: accounts with JWT login, profiles with an avatar and description, public profile pages.
- **Engagement**: bookmarks, threaded comments with up/down votes, and share links that show a preview when posted
  elsewhere.
- **Roles**: users manage their own recipes, comments and profile; admins can delete anyone's.

## Technology

| Area | Technology | Used for |
|---|---|---|
| Backend language | Java 25 | The API server |
| Backend framework | Spring Boot 4.1 (Web MVC, Validation, Actuator) | REST API, request validation, `/actuator/health` |
| Security | Spring Security + JJWT 0.12 | Stateless login with signed JWTs, BCrypt password hashing |
| Persistence | Spring Data JPA / Hibernate | Entities, repositories, and Criteria queries for search |
| Mapping & boilerplate | MapStruct, Lombok | Entity ↔ DTO mapping; getters, builders and constructors |
| Database | PostgreSQL (Neon in production, Docker locally) | All application data |
| File storage | Neon Object Storage via the AWS SDK v2 S3 client | Recipe photos and avatars, served from a public-read bucket |
| Test database | H2 (in memory) | Integration tests |
| Frontend | React 19 + TypeScript 6 | The single-page app in `frontend/granary-ui` |
| Frontend build | Vite 8, oxlint | Dev server with an API proxy, production build, linting |
| Frontend state | Redux Toolkit + RTK Query, React Router 7 | Server data caching per API, auth state, routing |
| Hosting | Railway (backend, built from the `Dockerfile`), Neon (database and storage) | Production |
| Automation | GitHub Actions | Hourly cleanup of the previous JWT secret after a rotation |

## Repository layout

```
.
├── src/main/java/com/example/granary
│   ├── web/          REST controllers, JWT filter, global exception handler, share-preview page
│   ├── business/     Services: recipes, search, tags, auth, users, comments, bookmarks, image storage
│   ├── repo/         Spring Data repositories and RecipeSpecifications (dynamic search queries)
│   ├── model/        JPA entities
│   ├── dto/          Request/response DTOs and the MapStruct RecipeMapper
│   ├── config/       Security, S3 client, dev and sample-data seeders
│   └── exceptions/   Domain exceptions mapped to HTTP status codes
├── src/main/resources
│   ├── application*.properties   Base, dev and prod configuration
│   └── db/seed-default-tags.sql  Default tags, applied at every startup
├── src/test/                    Unit, MockMvc and full-stack integration tests (H2)
├── frontend/granary-ui/         React app
├── scripts/
│   ├── sql/                     One-off migrations and seeds to run by hand
│   └── rotate-jwt-secret.ps1    Rotates JWT_SECRET on Railway
├── .github/workflows/           JWT secret cleanup job
├── Dockerfile                   Backend image used by Railway
└── docker-compose.yml           Local PostgreSQL
```

## How it fits together

```
Browser ──► React app (Vite dev server or static hosting)
              │  /api/*  (proxied to the backend in development)
              ▼
           Spring Boot API ──► PostgreSQL (recipes, users, tags, comments…)
              │
              └──► Neon Object Storage (photos)  ◄── browser loads image URLs directly
```

### Backend

- **Layers**: controllers in `web` validate input and return DTOs, services in `business` hold the rules, and
  repositories in `repo` talk to the database. `GlobalExceptionHandler` turns exceptions into JSON errors (`ApiError`)
  with the right status code: 400 for validation, 401, 403, 404, and 500 for anything unexpected.
- **Authentication**: `POST /api/auth/login` and `/register` return a JWT. The frontend sends it as
  `Authorization: Bearer …`, and `JwtAuthenticationFilter` authenticates each request; the server keeps no session.
  Most `GET` endpoints are public; writes and `/mine` endpoints need a token (see `SecurityConfig`). Tokens last 24 hours.
  `JWT_SECRET_PREVIOUS` lets tokens signed with the old secret keep working during a secret rotation.
- **Search**: `RecipeSpecifications` builds the query with the JPA Criteria API, because the number of words varies.
  Each word becomes an OR across title, description, tags and ingredient names (tags and ingredients via correlated
  `EXISTS` subqueries), the words are ANDed together, and a `CASE`-based score orders results by relevance. `%` and `_`
  in user input are escaped.
- **Tags**: `TagRules` defines the stored form (lowercase, letters/digits/underscores, at most 32 characters) and finds
  `#hashtags` in text. `TagService` applies the rules on every save. `recipe_tags` stores each recipe's tag names, and
  the `tags` table is the catalogue of available tags with their categories.
- **Images**: `ImageStorageService` uploads to the bucket with a random-ID file name and stores the public URL
  (`<endpoint>/<bucket>/<file>`), so images never pass through the API when they are viewed.
- **Schema**: Hibernate creates and updates tables (`ddl-auto=update`). Changes it can't make safely, such as moving
  data or adding a required column to a table that has rows, ship as hand-run scripts in `scripts/sql`.

### Frontend

- One RTK Query API per backend area (`store/recipeApi.ts`, `tagApi.ts`, `commentApi.ts`…). The shared
  `authedBaseQuery` adds the token, and on a 401 it logs the user out and sends them to the login page with a "session
  expired" message.
- Pages: home feed (`/`), recipe (`/recipes/:id`), search (`/search?q=`), tag (`/tag/:tag`), new and edit recipe,
  profile, public profile and login. The search query, tag and page number live in the URL, so results can be linked.
- Styling is plain CSS: design tokens (colours, fonts, spacing) in `styles/tokens.css` and component styles in
  `styles/app.css`. `src/app.css` is an old copy that nothing imports; don't add to it.

## Running locally

### Prerequisites

- JDK 25 (the Maven wrapper `./mvnw` handles Maven itself)
- Node.js 20.19+ or 22.12+
- Docker, for the local PostgreSQL. Any PostgreSQL 15+ also works.
- An S3-compatible bucket for images. A Neon branch with its own bucket is easiest; see [Configuration](#configuration).

### 1. Start PostgreSQL

```bash
export DB_PASSWORD=choose-a-password
docker compose up -d
```

This creates the database `granary` with user `granary` on `localhost:5432`.

### 2. Set the backend environment

The backend refuses to start without a JWT secret and storage settings.

```bash
export DB_PASSWORD=choose-a-password                 # same as above
export JWT_SECRET=$(openssl rand -base64 32)         # Base64, at least 32 bytes
export AWS_ENDPOINT_URL_S3=https://br-….storage….neon.tech
export AWS_REGION=us-east-2
export APP_STORAGE_BUCKET=granary-images-dev
export AWS_ACCESS_KEY_ID=…
export AWS_SECRET_ACCESS_KEY=…
```

On Windows PowerShell, use `$env:NAME = "value"` instead of `export`.

### 3. Run the backend

```bash
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080` with the `dev` profile (the default). On startup Hibernate creates the
tables, the default tags are seeded, and two dev accounts are created: `testuser` and `testadmin` (admin), both with
password `dev-password-not-for-prod`.

The ingredient picker needs the ingredient catalogue. Load it once into a fresh database:

```bash
docker exec -i granary-postgres psql -U granary -d granary < scripts/sql/seed-final-foods.sql
```

Optional sample data (users, recipes, comments, bookmarks): add the `seed-test-data` profile, e.g.
`./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,seed-test-data`. Seeded users have the password `TestPass123!`.

### 4. Run the frontend

```bash
cd frontend/granary-ui
npm install
npm run dev
```

Open `http://localhost:5173`. Vite proxies `/api` and `/images` to the backend on port 8080.

## Configuration

Spring profiles: `dev` (default, local PostgreSQL, verbose logging), `prod` (Railway; settings from environment
variables), `test` (H2, used by the test suite) and `seed-test-data` (adds sample data).

| Variable | Needed in | Purpose |
|---|---|---|
| `JWT_SECRET` | all, including tests | Base64 key (32+ bytes) that signs login tokens |
| `JWT_SECRET_PREVIOUS` | optional | Old secret still accepted during a rotation |
| `DB_PASSWORD` | dev, prod | Database password |
| `DATABASE_URL`, `DATABASE_USERNAME` | prod | JDBC URL and user for Neon |
| `DDL_AUTO` | prod, optional | Hibernate schema mode; defaults to `update`. Must stay `update` so new tables (e.g. `tags`) are created |
| `AWS_ENDPOINT_URL_S3`, `AWS_REGION` | dev, prod | Neon Object Storage endpoint and region (plain form, e.g. `us-east-2`) |
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` | dev, prod | Storage credential with read and write scopes |
| `APP_STORAGE_BUCKET` | dev, prod | Bucket name; the bucket must be public-read |
| `APP_CORS_ALLOWED_ORIGINS` | prod | Frontend origin(s) allowed to call the API |
| `APP_FRONTEND_BASE_URL` | prod | Frontend URL that share-preview pages link back to |
| `PORT` | prod, optional | HTTP port; Railway sets it |
| `VITE_SHARE_BASE_URL` | frontend | Base URL used when copying share links |

## Testing

```bash
./mvnw test                         # needs JWT_SECRET in the environment
cd frontend/granary-ui
npm run lint
npm run build                       # type-checks (tsc -b) and builds
```

Backend tests run against in-memory H2 and include unit tests (services, rules, mapper), MockMvc controller tests, and
full-stack integration tests that start the app on a random port (`BaseIntegrationTest`). They don't cover
PostgreSQL-only behaviour, so check database changes against a Neon branch before production.

## Database scripts

| Script | When it runs | Purpose |
|---|---|---|
| `src/main/resources/db/seed-default-tags.sql` | Automatically at every startup | Inserts missing default tags and corrects their categories. Safe to re-run. To change the defaults, edit this file |
| `scripts/sql/seed-final-foods.sql` | By hand, once per database | Fills the ingredient suggestion catalogue |
| `scripts/sql/migrate-ingredient-suggestions.sql` | By hand, once (already applied in production) | Moved free-text ingredients to suggestion references |
| `scripts/sql/normalize-recipe-tags.sql` | By hand, once, after deploying the tag feature | Cleans tags saved before the hashtag rules existed |

Hand-run scripts are written to be re-runnable. Before running one in production, create a Neon branch from
`production` as a backup.

## Deployment

- **Backend**: Railway builds the `Dockerfile` (Maven build, then a Java 25 runtime image) and runs it with the `prod`
  profile and the environment variables above. Startup creates or updates tables and applies the default tag seed.
- **Database and images**: Neon, on the `production` branch. Credentials for object storage are per branch.
- **Frontend**: built with `npm run build` and deployed separately from the backend; the host must route `/api` to the
  backend.
- **Order**: deploy the backend before a frontend that depends on new endpoints. Run any hand-run SQL script for the
  release right after the backend deploy, and rehearse on a Neon branch first.
- **JWT secret rotation**: `scripts/rotate-jwt-secret.ps1 -RailwayEnvironment production` moves the current secret to
  `JWT_SECRET_PREVIOUS` and sets a new one. The `jwt-secret-cleanup` workflow removes the previous secret 24 hours
  later, once old tokens have expired.

## Working on the code

- Work happens on a branch per Jira ticket, named after it (`KAN-23`), with commit messages starting with the ticket
  key (`KAN-23: Added hashtagging for recipes`). Pull requests merge into `master`.
- New files are easy to miss: check `git status` for untracked files before committing. The frontend build breaks if a
  newly imported file isn't committed.
- Comments: classes and non-obvious public methods get a short Javadoc/JSDoc summary; avoid inline comments that
  restate the code.
- Merge `master` into a long-running branch before deploying it. A branch that predates a schema change will fail
  against the migrated database.
