# Server: Firebase sign-in

Implemented in the web API (`E:\Drama Local Project\Scrapped-Data\web`):

| File | What it does |
|---|---|
| `app/api/user/firebase-session/route.ts` | `POST /api/user/firebase-session` – trades a verified Firebase ID token for an app session |
| `lib/firebaseAuth.ts` | Verifies Firebase ID tokens against Google's public keys (like `googleAuth.ts`; no firebase-admin) |
| `lib/db.ts` → `resolveFirebaseUser` | Maps the verified UID to an account in one transaction; schema v8 adds `users.firebase_uid` (unique) |

## How accounts map (coins stay safe)

1. Account already bound to this Firebase UID → that account (Google link recorded when present).
2. Account already linked to the same Google account (older `/login-google` flow) → bind the UID to it.
3. `legacyUserId` from a pre-auth install that was never claimed → bind the UID to it, once.
4. Otherwise → new account with `user_id = <Firebase UID>` and the signup bonus.

Linking Google in the app keeps the Firebase UID, so step 1 always finds the same row: nothing is
moved or merged. The UID is only ever taken from a verified token.

## Request / responses

```
POST /api/user/firebase-session
{ "firebaseIdToken": "...", "userId": "<Firebase UID>", "legacyUserId"?: "...", "clientDate"?: "2026-10-27" }

200 { ...profile, accessToken, accessTokenExpiresIn, refreshToken, refreshTokenExpiresIn, created }
401 FIREBASE_TOKEN_INVALID   409 ACCOUNT_ALREADY_CLAIMED   409 GOOGLE_ACCOUNT_IN_USE
```

## Config

- `FIREBASE_PROJECT_ID` (optional; defaults to `short-rewards-app`, the project in `google-services.json`).
- Firebase console: enable **Anonymous** and **Google** providers; add SHA-1/SHA-256 for every signing key.
- Leave automatic deletion of anonymous accounts off.
