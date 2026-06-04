/**
 * HTTP endpoint: admobSsvCallback
 *
 * AdMob Rewarded Server-Side Verification (SSV). When a user finishes a
 * rewarded ad, AdMob calls THIS url directly (server→server) with a signed
 * query string. We verify Google's ECDSA signature, then grant credits to the
 * user named in `custom_data`. Because the grant originates from Google — not
 * the client — a tampered app cannot forge rewarded credits.
 *
 * SETUP (one-time):
 *  1. Deploy this function and copy its URL.
 *  2. AdMob console → your Rewarded ad unit → Server-side verification →
 *     paste the URL as the SSV callback.
 *  3. On the client, set ServerSideVerificationOptions.customData = the user's
 *     uid before showing the ad (see RewardedAdManager).
 *  4. Flip USE_ADMOB_SSV to true in config.ts (backend) and CreditConfig
 *     (client) and redeploy. The client then stops calling the grant callable.
 *
 * Docs: https://developers.google.com/admob/android/ssv
 */
import {createPublicKey, verify as cryptoVerify} from "crypto";
import {onRequest} from "firebase-functions/v2/https";
import {ADMOB_SSV_MAX_AGE_MS, ADMOB_VERIFIER_KEYS_URL, REGION} from "./config";
import {ensureUserDoc, grantRewardedAdCreditsForUser} from "./credits";

interface VerifierKey {
  keyId: number;
  pem: string;
  base64: string;
}

// Cache Google's public verifier keys (they rotate slowly).
let keyCache: {fetchedAt: number; keys: Map<string, string>} | null = null;
const KEY_TTL_MS = 24 * 60 * 60 * 1000;

async function getVerifierKey(keyId: string): Promise<string | null> {
  if (!keyCache || Date.now() - keyCache.fetchedAt > KEY_TTL_MS) {
    const res = await fetch(ADMOB_VERIFIER_KEYS_URL);
    const json = (await res.json()) as {keys: VerifierKey[]};
    const map = new Map<string, string>();
    for (const k of json.keys) map.set(String(k.keyId), k.pem);
    keyCache = {fetchedAt: Date.now(), keys: map};
  }
  return keyCache.keys.get(keyId) ?? null;
}

export const admobSsvCallback = onRequest(
  {region: REGION, cors: false},
  async (req, res) => {
    try {
      // Work on the RAW query string — the signature is computed over the exact
      // bytes AdMob sent, so we must not let any parser reorder/re-encode it.
      const qIndex = req.url.indexOf("?");
      const rawQuery = qIndex >= 0 ? req.url.slice(qIndex + 1) : "";
      const sigMarker = "&signature=";
      const sigIdx = rawQuery.indexOf(sigMarker);
      if (sigIdx < 0) {
        res.status(400).send("missing signature");
        return;
      }
      // Content to verify = everything before "&signature=" (signature & key_id
      // are always the final two params, in that order).
      const content = rawQuery.slice(0, sigIdx);

      const params = new URLSearchParams(rawQuery);
      const signatureB64 = params.get("signature");
      const keyId = params.get("key_id");
      if (!signatureB64 || !keyId) {
        res.status(400).send("missing signature/key_id");
        return;
      }

      const pem = await getVerifierKey(keyId);
      if (!pem) {
        res.status(400).send("unknown key_id");
        return;
      }

      // AdMob uses URL-safe base64 for the (DER-encoded) ECDSA signature.
      const sig = Buffer.from(
        signatureB64.replace(/-/g, "+").replace(/_/g, "/"),
        "base64"
      );
      const verified = cryptoVerify(
        "sha256",
        Buffer.from(content, "utf8"),
        createPublicKey(pem),
        sig
      );
      if (!verified) {
        // A bad signature means a forged/tampered request — reject hard.
        res.status(403).send("invalid signature");
        return;
      }

      // Replay defense: reject stale callbacks (transaction_id dedup is the
      // primary guard; the timestamp check is belt-and-braces).
      const tsMicros = Number(params.get("timestamp"));
      if (
        Number.isFinite(tsMicros) &&
        Date.now() - tsMicros / 1000 > ADMOB_SSV_MAX_AGE_MS
      ) {
        res.status(400).send("stale callback");
        return;
      }

      const uid = params.get("custom_data") || params.get("user_id");
      const transactionId = params.get("transaction_id");
      if (!uid || !transactionId) {
        res.status(400).send("missing uid/transaction_id");
        return;
      }

      await ensureUserDoc(uid);
      // Idempotent on transaction_id — AdMob may retry the callback.
      await grantRewardedAdCreditsForUser(uid, 0, `ssv_${transactionId}`);

      res.status(200).send("OK");
    } catch (err) {
      // eslint-disable-next-line no-console
      console.error("admobSsvCallback error", err);
      res.status(500).send("error"); // non-200 → AdMob retries
    }
  }
);
