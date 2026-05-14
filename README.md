# Orchestrator Android

Native Android task manager for the [Orchestrator](https://github.com/kjastram/orchestrator) backend. Built with Kotlin + Jetpack Compose. No Google services — runs on /e/OS (Fairphone 5).

## Requirements

- Android Studio Ladybug or later
- Android SDK 26+
- Java 17
- `gh` CLI (for secrets setup)

## Development Setup

```bash
git clone https://github.com/kjastram/orchestrator-android
cd orchestrator-android
./gradlew assembleDebug
```

The debug APK is output to `app/build/outputs/apk/debug/app-debug.apk`.

To sideload to a device:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

The app points to `https://orchestrator-qn5ab.ondigitalocean.app` — no local backend setup needed.

## Release Setup (one-time)

### 1. Generate a keystore

```bash
keytool -genkey -v -keystore ~/orchestrator-android.jks \
  -alias orchestrator -keyalg RSA -keysize 2048 -validity 10000
```

Keep this file safe — losing it means you can't release updates to the same app ID.

### 2. Add GitHub secrets

```bash
gh secret set KEYSTORE_BASE64 \
  --repo kjastram/orchestrator-android \
  --body "$(base64 -i ~/orchestrator-android.jks)"

gh secret set KEYSTORE_PASSWORD \
  --repo kjastram/orchestrator-android \
  --body "your-keystore-password"

gh secret set KEY_ALIAS \
  --repo kjastram/orchestrator-android \
  --body "orchestrator"

gh secret set KEY_PASSWORD \
  --repo kjastram/orchestrator-android \
  --body "your-key-password"
```

### 3. Set up Obtainium (on device)

1. Install [Obtainium](https://github.com/ImranR98/Obtainium)
2. Add source: `https://github.com/kjastram/orchestrator-android`
3. Filter releases by: `app-release.apk`
4. Enable background updates

## Shipping a Release

```bash
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions builds the signed APK and publishes it as a GitHub Release. Obtainium prompts the install on device automatically.

## Architecture

```
ui/
  login/        LoginScreen + LoginViewModel
  tasks/        TaskListScreen, TaskEditSheet + ViewModels
  theme/        Material3 dynamic color theme
nav/            AppNavGraph (single-activity Compose Nav)
data/
  api/          Retrofit ApiService + AuthInterceptor
  model/        Task, LoginRequest/Response, TaskCreate/Update
  repository/   AuthRepository, TaskRepository
  store/        TokenStore (EncryptedSharedPreferences)
di/             Hilt AppModule
notification/   TaskAlarmReceiver + NotificationScheduler
```

## Key Tech

| Concern | Library |
|---------|---------|
| UI | Jetpack Compose + Material3 |
| Navigation | Navigation-Compose |
| DI | Hilt |
| Networking | Retrofit 2 + OkHttp |
| Auth storage | EncryptedSharedPreferences |
| Notifications | AlarmManager (no FCM) |
