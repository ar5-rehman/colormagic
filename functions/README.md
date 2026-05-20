# ColorMagic Kids — Firebase backend

Cloud Functions (v2, TypeScript) for sketch generation, credits and billing.
The Android app calls these as **callable functions** — the Firebase SDK
auto-attaches the user's Auth token and an App Check token on every call, so
the OpenAI key never ships in the app and only the genuine build can call in.

## Functions

| Function | Purpose |
|---|---|
| `generateSketch` | Safety-checks the prompt, picks the model from the credit bucket, calls OpenAI, uploads the PNG, deducts 1 credit. Returns `{ sketchId, imageUrl }`. |
| `userQuota` | Returns remaining credits → drives "Sketches left: N". |
| `verifyPurchase` | Server-side Google Play verification → updates plan / credits. |

## One-time setup

1. **Install the Firebase CLI** and log in:
   ```
   npm i -g firebase-tools && firebase login
   ```
2. **Install deps:**
   ```
   cd functions && npm install
   ```
3. **Set the OpenAI key** as a Functions secret (never an env file):
   ```
   firebase functions:secrets:set OPENAI_API_KEY
   ```
4. **Verify the model IDs** in `src/config.ts` (`MODEL_FREE`, `MODEL_PREMIUM`)
   against the current OpenAI Image API — names change.
5. **Google Play verification** (for `verifyPurchase`):
   - Enable the *Google Play Android Developer API* in Google Cloud.
   - In Play Console → Users & permissions, invite this project's Functions
     runtime service account and grant it app/financial access.
   - Create the products in Play Console: subscription `monthly_pro`,
     in-app `extra_20_sketches`.
6. **Register the App Check debug token** (for local Android debug builds):
   run the debug app once, copy the token Logcat prints, add it under
   Firebase Console → App Check → Apps.

## Build & deploy

```
npm run build            # type-check / compile to lib/
firebase deploy --only functions
firebase deploy --only firestore:rules,storage:rules,firestore:indexes
```

## Client contract (Android)

Call via `FirebaseFunctions` callables (region `us-central1`):

```kotlin
// generateSketch
data:    { "prompt": "a cute dinosaur eating an apple" }
result:  { "success": true, "sketchId": "...", "imageUrl": "https://..." }

// userQuota
data:    {}
result:  { "plan", "subscriptionActive", "remainingFreeSketches",
           "remainingMonthlySketches", "extraCredits", "totalAvailableCredits" }

// verifyPurchase
data:    { "productId": "monthly_pro", "purchaseToken": "..." }
result:  { "success": true, "plan", "totalAvailableCredits" }
```

Errors arrive as `FirebaseFunctionsException` — notable codes:
- `resource-exhausted` → no credits ("Ask a grown-up to unlock more.")
- `invalid-argument` → unsafe/empty prompt (message is kid-safe to show)
- `unauthenticated` → no signed-in user
