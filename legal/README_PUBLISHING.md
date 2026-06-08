# Legal docs — how to publish & stay Play-compliant

These documents are templates tailored to Color Magic Kids. **They are not legal advice.**
Because the App targets children, have them reviewed by a lawyer before publishing.

## 1. Fill in the placeholders

Search both files for `[` and replace every bracketed placeholder:

- `[Your Legal Company / Developer Name]`
- `[your-support-email@example.com]`
- `[Mailing address, optional]`
- `[Your Country / State]` and `[Your Jurisdiction]` (Terms, governing law)
- `[https://colormagic.app/delete-account]` (account-deletion page URL)

## 2. Host them at the URLs the app already uses

The app links to (see `app/.../presentation/util/AppLinks.kt`):

- Terms:   `https://colormagic.app/terms`
- Privacy: `https://colormagic.app/privacy`

The URLs must be **public, stable, and not behind a login**. Options:
- Convert these `.md` files to simple HTML pages on your own site, or
- GitHub Pages / Cloudflare Pages / Firebase Hosting, then point the paths above to them.

If your final URLs differ, update `AppLinks.TERMS_URL` / `AppLinks.PRIVACY_URL` to match.
The **Privacy Policy URL in the app must match the one in the Play Console.**

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
**in-app** path and a **web URL** to request account + data deletion. The Privacy
Policy references `[https://colormagic.app/delete-account]` — create that page.

## 4. Open items in the app to confirm before release

- **App Check enforcement**: `functions/src/config.ts` → set `ENFORCE_APP_CHECK = true`
  and register real tokens before publishing.
- **AdMob IDs** are still test placeholders — swap for real IDs.
- Verify the rewarded-ads integration requests **non-personalized** ads for children.
- Make sure the **Data Safety** answers match this Privacy Policy exactly.

## 5. Keep the dates current

Update "Last updated" whenever you change either document, and re-publish.
