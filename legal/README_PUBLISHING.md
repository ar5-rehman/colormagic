# Legal docs — how to publish & stay Play-compliant

These documents are templates tailored to Color Magic Kids. **They are not legal advice.**
Because the App targets children, have them reviewed by a lawyer before publishing.

## 1. Sources

- `PRIVACY_POLICY.md`, `TERMS_AND_CONDITIONS.md`, `DELETE_ACCOUNT.md` — edit these.
- Publisher: ZyloMind · Contact: abdurzylomind@gmail.com · Governing law: Pakistan.
  "ZyloMind" must match the developer name shown on the Play Console account.

## 2. Build & host (Firebase Hosting, free)

```
python3 legal/build_html.py      # writes hosting/public/*.html
firebase deploy --only hosting
```

Public URLs (already used by the app in `AppLinks.kt`):

- Privacy:        https://colormagic-555.web.app/privacy
- Terms:          https://colormagic-555.web.app/terms
- Delete account: https://colormagic-555.web.app/delete-account

## 3. Google Play Console — what actually gets checked

A policy text alone does NOT make a kids' app pass review. Align these:

### a) Privacy policy field
Play Console → App content → **Privacy policy** → paste the `…/privacy` URL.

### b) Data safety form (must match the Privacy Policy)
Declare what the app collects/shares. Based on the current code, expect to declare:
- **App activity / app info & performance** — diagnostics.
- **Personal info: Email address, Name, Photo** — *only collected if* a parent uses
  Google Sign-In.
- **Financial info: Purchase history** — via Google Play Billing (token/entitlement; no
  card data).
- **Device or other IDs** — for ads/anti-abuse.
- Data is **encrypted in transit**; provide the **account-deletion URL**.
- Do **not** mark data as used for personalized advertising.

### c) Target audience & content (Families)
App content → **Target audience and content**:
- Select an age group that **includes children**.
- This puts the app in the **Families program**, which requires the **Designed for
  Families** requirements and a **compliant ads SDK config**.

### d) Ads declaration
- Declare that the app **contains ads**.
- For child-directed apps you must use **non-personalized ads** and an ad SDK/config
  certified for families. In AdMob, enable **"tag for child-directed treatment"
  (TFCD)** and request non-personalized ads. (Confirm your AdMob/UMP setup does this in
  code before release.)

### e) Account deletion (required when accounts exist)
Because the app creates an account (anonymous + optional Google), Play requires an
**in-app** path and a **web URL** to request account + data deletion.
Web URL: https://colormagic-555.web.app/delete-account. In-app path: Parent Area →
Help & Support (feedback form identifies the account by uid).

## 4. Open items in the app to confirm before release

- **App Check enforcement**: `functions/src/config.ts` → set `ENFORCE_APP_CHECK = true`
  and register real tokens before publishing.
- **AdMob IDs** are still test placeholders — swap for real IDs.
- Verify the rewarded-ads integration requests **non-personalized** ads for children.
- Make sure the **Data Safety** answers match this Privacy Policy exactly.

## 5. Keep the dates current

Update "Last updated" whenever you change either document, and re-publish.
