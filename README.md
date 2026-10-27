# DramaFlix (Android)

Jetpack Compose app that browses the same Next.js API as the `web/` catalog.

## Requirements

- Android Studio Ladybug+ / JDK 17
- Emulator or device
- Next.js API running (`cd web && npm run dev`)

## API base URL

Default (all builds): `https://short-rewards-web-api.vercel.app/`

Override in `local.properties` (git-ignored):

```
api.base.url=https://your-api.example.com/      # all builds
api.base.url.debug=http://10.0.2.2:3000/        # debug only (emulator -> local Next.js)
```

Plain HTTP is allowed only to `10.0.2.2` / `localhost` (see `network_security_config.xml`).

## Release signing

Signing secrets are read from `local.properties` (or env vars `SIGNING_STOREFILE`,
`SIGNING_STOREPASSWORD`, `SIGNING_KEYALIAS`, `SIGNING_KEYPASSWORD`), never from Gradle files:

```
signing.storeFile=math.jks
signing.storePassword=...
signing.keyAlias=key0
signing.keyPassword=...
```

If they're missing, the release build is produced unsigned.

## Networking & caching

- One shared Ktor client (OkHttp engine: HTTP/2, connection pool, 20 MB HTTP cache).
- Non-2xx responses throw `ApiException` with the server's `error`/`message`, so screens can show `e.message`.
- GETs retry up to 2x with exponential backoff on network errors, 5xx and 429. POSTs (rewards, check-in, redemption) are never retried automatically.
- Room is an offline cache: list membership (`drama_list_entries`, `reel_list_entries`) is separate from item content, writes are transactional, and old search lists / orphaned rows are pruned on start.
- `UserRepository.sync()` is single-flight and throttled to once per 30 s (`sync(force = true)` to bypass).

## Accounts & Google sign-in

- Identity is **Firebase Authentication**. *Continue as guest* signs in to Firebase
  anonymously; *Back up with Google* links Google to that same Firebase user, so the UID,
  the server account and its coins never change. *Sign in with Google* on a new phone
  returns the same UID.
- The app trades a verified Firebase ID token (plus the UID) for its own access/refresh
  tokens at `POST /api/user/firebase-session`. The server must verify the token; see
  **SERVER_FIREBASE_AUTH.md** for the endpoint, rules and route code.
- Installs from before auth keep their account: the saved user id is sent once as
  `legacyUserId`. If it's already claimed (`409 ACCOUNT_ALREADY_CLAIMED`) the app asks the
  user to sign in with Google.
- App tokens are encrypted with an Android Keystore key and excluded from backup. Every call
  sends `Authorization: Bearer`; the access token is refreshed 60 s before expiry and on
  `401 TOKEN_*` (one refresh at a time). If the refresh token is rejected, the app silently
  gets new tokens from Firebase; only if that fails does it show the sign-in screen.
- Rewards and check-ins send a claim ID (`Idempotency-Key` + `claimId`), saved before
  sending and retried with the same ID when the result is unknown. `409 ALREADY_CLAIMED`
  counts as success. Reward calls use `?lite=1`.

### Setup

1. Firebase console: add the Android app `com.dramflix.android` with the SHA-1 and SHA-256 of
   every signing key (debug via `./gradlew signingReport`, `math.jks`, Play App Signing).
   Enable the **Anonymous** and **Google** sign-in providers.
2. Download `google-services.json` into `app/` (again after adding SHA keys or enabling
   Google). The google-services plugin reads the Firebase config and the Web client ID
   (`default_web_client_id`) from it; nothing goes in `local.properties`.
3. Deploy `/api/user/firebase-session` on the server (SERVER_FIREBASE_AUTH.md).
4. Optional: add Google's official "G" logo from their Sign-In branding page as
   `app/src/main/res/drawable/ic_google_logo.xml` (or `.png`); the button picks it up.

## Release build & start-up

- `assembleRelease` runs **R8** (code shrinking + obfuscation) and **resource shrinking**.
  Keep rules: `app/proguard-rules.pro`; runtime-looked-up resources: `res/raw/keep.xml`.
  Keep `app/build/outputs/mapping/release/mapping.txt` for each release to read crash
  stack traces (upload it to Play Console with the bundle).
- Cold start: `Application.onCreate` only wires lazy singletons. The HTTP client is built
  on a background thread, the database opens off the critical path, cache pruning and
  reward retries wait 5 s, Firebase starts only when sign-in needs it, and WorkManager
  auto-init is disabled. The window background is dark from the first frame.
- `src/main/baseline-prof.txt` + ProfileInstaller pre-compile the start-up path.
  For a measured profile, add a Baseline Profile module (Android Studio → New Module →
  Baseline Profile Generator) later.
- If a release build crashes but debug doesn't, it's almost always a missing keep rule:
  check Logcat for `ClassNotFoundException` / `SerializationException` and add a `-keep`.

## Run

1. Start the web API: `cd ../web && npm run dev`
2. Open `AndroidDramaApp` in Android Studio
3. Sync Gradle → Run on emulator/device

## Features

- Catalog search + cursor **Load more**
- Drama detail (cover, synopsis, tags, episodes)
- HLS playback via Media3 ExoPlayer using `episodes.playUrl` from the API
