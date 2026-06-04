/**
 * Shared Google Play Android Publisher client.
 *
 * Authenticated via Application Default Credentials (the function's runtime
 * service account). One-time setup:
 *  1. Enable the "Google Play Android Developer API" in Google Cloud.
 *  2. In Play Console → Users & permissions, invite the runtime service
 *     account and grant it access to financial data / the app.
 */
import {Timestamp} from "firebase-admin/firestore";
import {google} from "googleapis";
import {ANDROID_PACKAGE_NAME} from "./config";

/** Android Publisher v3 client, authed via the function's service account. */
export function androidPublisher() {
  const auth = new google.auth.GoogleAuth({
    scopes: ["https://www.googleapis.com/auth/androidpublisher"],
  });
  return google.androidpublisher({version: "v3", auth});
}

export interface SubscriptionState {
  /** Whether the user is currently entitled to premium. */
  active: boolean;
  /** End of the currently-paid period. */
  expiresAt: Timestamp | null;
}

/**
 * Fetches the AUTHORITATIVE current state of a subscription from Google Play.
 *
 * IMPORTANT entitlement rule: a CANCELED subscription is still ACTIVE here —
 * cancelling only turns off auto-renew; the user keeps premium until the period
 * they already paid for ends (Play then reports EXPIRED). We only drop premium
 * on EXPIRED / ON_HOLD / PAUSED, and immediately on REVOKED (refund/chargeback).
 */
export async function fetchSubscriptionState(
  purchaseToken: string
): Promise<SubscriptionState> {
  const res = await androidPublisher().purchases.subscriptionsv2.get({
    packageName: ANDROID_PACKAGE_NAME,
    token: purchaseToken,
  });

  const state = res.data.subscriptionState;
  const active =
    state === "SUBSCRIPTION_STATE_ACTIVE" ||
    state === "SUBSCRIPTION_STATE_CANCELED" || // paid-through; keep until expiry
    state === "SUBSCRIPTION_STATE_IN_GRACE_PERIOD";

  const lineItems = res.data.lineItems ?? [];
  const expiryIso = lineItems[lineItems.length - 1]?.expiryTime;
  const expiresAt = expiryIso ? Timestamp.fromDate(new Date(expiryIso)) : null;

  return {active, expiresAt};
}
