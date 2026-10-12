/**
 * Keeps one account per phone across uninstall/reinstall.
 *
 * Guest (anonymous) Firebase accounts live only in the app's local storage, so
 * uninstalling would otherwise mint a brand-new account — with a fresh free
 * credit, an empty streak, and none of the old purchased credits. Instead the
 * app sends a deviceKey (SHA-256 of an app salt + the Android ID, which
 * survives reinstalls and is unique per app-signing key) and we map it to the
 * account last used on that phone.
 *
 *  • registerDevice (signed in): remember "this phone → my uid".
 *  • restoreDeviceAccount (fresh install, not signed in yet): if this phone
 *    has an account, return a Firebase custom token for it; the app signs in
 *    with that token and is back on the same uid.
 *
 * Needs (one-time): the functions' service account must be able to sign
 * custom tokens — grant it "Service Account Token Creator" on itself and
 * enable the IAM Service Account Credentials API.
 */
import {getAuth} from "firebase-admin/auth";
import {Timestamp} from "firebase-admin/firestore";
import {HttpsError, onCall} from "firebase-functions/v2/https";
import {DEFAULT_MAX_INSTANCES, ENFORCE_APP_CHECK, REGION, WELCOME_CREDITS_PER_DAY} from "./config";
import {Collections, db} from "./firebase";

/** An account counts as "just created on this phone" within this window. */
const NEW_ACCOUNT_WINDOW_MS = 10 * 60 * 1000;

/** 64 lowercase hex chars (SHA-256). Anything else is rejected. */
export function parseDeviceKey(raw: unknown): string {
  const key = typeof raw === "string" ? raw.trim().toLowerCase() : "";
  if (!/^[0-9a-f]{64}$/.test(key)) {
    throw new HttpsError("invalid-argument", "Invalid device.");
  }
  return key;
}

export const registerDevice = onCall(
  {region: REGION, maxInstances: DEFAULT_MAX_INSTANCES, enforceAppCheck: ENFORCE_APP_CHECK},
  async (request): Promise<{success: true}> => {
    const uid = request.auth?.uid;
    if (!uid) throw new HttpsError("unauthenticated", "Sign-in required.");
    const key = parseDeviceKey((request.data ?? {}).deviceKey);
    // merge: keeps the phone's credit counters and any deleted-account info.
    // firstUid = the first account ever created on this phone — the only one
    // that gets free daily credits (see ensureUserDoc).
    const ref = db.collection(Collections.devices).doc(key);
    const firstUid = await db.runTransaction(async (tx) => {
      const snap = await tx.get(ref);
      const existing = snap.get("firstUid") as string | undefined;
      tx.set(ref, {
        uid,
        ...(existing ? {} : {firstUid: uid}),
        updatedAt: Timestamp.now(),
      }, {merge: true});
      return existing ?? uid;
    });

    // Race guard: the app may fetch credits for a brand-new account a moment
    // before this registration lands, so its users doc could have been created
    // with a free daily credit. If that account isn't this phone's first and
    // was JUST created, take the free credits back. (Older accounts — e.g. an
    // existing Google account signing in on this phone — are never touched.)
    if (firstUid !== uid) {
      const userRef = db.collection(Collections.users).doc(uid);
      const user = await userRef.get();
      const created = user.get("createdAt") as Timestamp | undefined;
      if (user.exists && user.get("freeCreditsEligible") !== false &&
          created && Date.now() - created.toMillis() < NEW_ACCOUNT_WINDOW_MS) {
        const welcome = (user.get("welcomeDaysGranted") as number ?? 0) * WELCOME_CREDITS_PER_DAY;
        await userRef.update({
          freeCreditsEligible: false,
          adCredits: Math.max(0, (user.get("adCredits") as number ?? 0) - welcome),
          welcomeDaysGranted: 0,
          ...(user.get("plan") === "pro" ? {} : {dailyCreditsAvailable: 0}),
          updatedAt: Timestamp.now(),
        });
      }
    }
    return {success: true};
  }
);

export const restoreDeviceAccount = onCall(
  {region: REGION, maxInstances: DEFAULT_MAX_INSTANCES, enforceAppCheck: ENFORCE_APP_CHECK},
  async (request): Promise<{token: string | null}> => {
    const key = parseDeviceKey((request.data ?? {}).deviceKey);
    const ref = db.collection(Collections.devices).doc(key);
    const uid = (await ref.get()).get("uid") as string | undefined;
    if (!uid) return {token: null};

    try {
      await getAuth().getUser(uid);
    } catch {
      // That account was deleted — forget the mapping; the app starts fresh.
      await ref.delete().catch(() => undefined);
      return {token: null};
    }
    return {token: await getAuth().createCustomToken(uid)};
  }
);
