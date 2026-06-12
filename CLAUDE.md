# Orchestrator Android — Claude Code Context

Native Android **client** for the Orchestrator backend. Kotlin + Jetpack Compose, single-activity.
No Google services (FCM, Play Services) — targets /e/OS on a Fairphone 5.

- **Repo:** `kjastram/orchestrator-android`, branch: **`master`** (note: backend repo uses `main`)
- **Backend it talks to:** `https://orchestrator-qn5ab.ondigitalocean.app` (prod only — no local backend needed)
  - Base URL is hardcoded in `app/src/main/kotlin/com/orchestrator/app/di/AppModule.kt` (`BASE_URL`).
- **Sibling repo:** the backend + API contract lives in `../orchestrator/` — see the workspace `../CLAUDE.md` for the cross-repo contract.

## Build & Run

```bash
./gradlew assembleDebug                 # debug APK → app/build/outputs/apk/debug/app-debug.apk
adb install app/build/outputs/apk/debug/app-debug.apk
```

- Android Studio Ladybug+, Android SDK 26+, **Java 17**.
- The debug app points at the prod backend; no local server setup.

## Project Structure

```
app/src/main/kotlin/com/orchestrator/app/
├── ui/
│   ├── login/        LoginScreen + LoginViewModel
│   ├── tasks/        TaskListScreen, TaskEditSheet + ViewModels
│   └── theme/        Material3 dynamic color theme
├── nav/              AppNavGraph (single-activity Compose Nav)
├── data/
│   ├── api/          ApiService (Retrofit), AuthInterceptor
│   ├── model/        Task.kt, Category.kt, Login/TaskCreate/TaskUpdate DTOs
│   ├── repository/   AuthRepository, TaskRepository
│   └── store/        TokenStore (EncryptedSharedPreferences)
├── di/               Hilt AppModule (Retrofit/OkHttp wiring, BASE_URL)
└── notification/     TaskAlarmReceiver + NotificationScheduler (AlarmManager)
```

## Key Tech

| Concern | Library |
|---------|---------|
| UI | Jetpack Compose + Material3 |
| Navigation | Navigation-Compose |
| DI | Hilt |
| Networking | Retrofit 2 + OkHttp (`AuthInterceptor` adds JWT bearer) |
| Auth storage | EncryptedSharedPreferences (`TokenStore`) |
| Notifications | AlarmManager — **no FCM** |

## API Contract (must stay in sync with backend)

`data/api/ApiService.kt` mirrors backend routes in `../orchestrator/backend/app/routers/`:

| ApiService | Backend router |
|-----------|----------------|
| `POST auth/login` | `auth.py` |
| `api/tasks` (GET/POST), `api/tasks/{id}` (PATCH/DELETE), `api/tasks/reorder` (POST), `api/tasks/{id}/move` (POST) | `tasks.py` |
| `api/task-categories` (GET/POST), `api/task-categories/{id}` (PATCH/DELETE) | `task_categories.py` |
| `api/accounts`, `api/accounts/net-worth`, `api/transactions*`, `api/budgets*`, `api/rules*` (finance dashboard) | `accounts.py`, `transactions.py`, `budgets.py`, `rules.py` |
| `api/plaid/sync` (POST), `api/plaid/sync-status` (GET) | `plaid.py` |

- `api/tasks/{id}/move` nests a task under a parent (`parent_id`) or promotes it to top-level (`to_top_level=true`). It exists because Gson omits null fields, so `PATCH` cannot clear `parent_id`.
- `api/plaid/sync-status` returns the backend-persisted last-successful-sync timestamp (shared across web + Android) and a `last_sync_error` flag.

`Task.kt` / `Category.kt` are the wire DTOs — if backend request/response schemas change, update these to match.

## Releasing

```bash
git tag v1.0.0 && git push origin v1.0.0
```

GitHub Actions builds a **signed** release APK and publishes a GitHub Release; Obtainium on-device picks it up.
Signing needs repo secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` (one-time setup — see `README.md`).
Keep the keystore (`~/orchestrator-android.jks`) safe; losing it blocks same-app-ID updates.
