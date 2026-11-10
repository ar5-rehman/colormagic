/**
 * Pub/Sub handler: playRtdnHandler
 *
 * Google Play Real-time Developer Notifications (RTDN). Play publishes
 * subscription lifecycle events (renew / cancel / refund / revoke / expire) to
 * a Pub/Sub topic; this function reacts to them and keeps the user's "pro"
 * entitlement honest. Without this, a user could subscribe, get refunded, and
 * keep premium forever — this revokes access the moment Play says it's gone.
 *
 * SETUP (one-time):
 *  1. Create the Pub/Sub topic named by PLAY_RTDN_TOPIC.
 *  2. Play Console → Monetization setup → Real-time developer notifications →
 *     set the topic name. Grant Play permission to publish to the topic.
 *  3. Deploy. (No client changes needed.)
 *
 * We never trust the notification's contents for entitlement — on every event
 * we RE-QUERY the authoritative subscription state from the Play API.
 *
 * Docs: https://developer.android.com/google/play/billing/rtdn-reference
 */
import {onMessagePublished} from "firebase-functions/v2/pubsub";
import {PLAY_RTDN_TOPIC, REGION, DEFAULT_MAX_INSTANCES} from "./config";
import {setSubscriptionState} from "./credits";
import {Collections, db} from "./firebase";
import {fetchSubscriptionState} from "./playApi";
import {logServerEvent} from "./analytics";

interface DeveloperNotification {
  subscriptionNotification?: {
    notificationType: number;
    purchaseToken: string;
    subscriptionId: string;
  };
  testNotification?: {version: string};
}

/** Play RTDN subscription notificationType → analytics event name. */
const RTDN_EVENT_NAMES: Record<number, string> = {
  1: "subscription_recovered",   // back from account hold
  2: "subscription_renewed",
  3: "subscription_cancelled",   // auto-renew turned off (still paid-through)
  5: "subscription_on_hold",     // payment failed, access paused
  6: "subscription_in_grace_period",
  7: "subscription_restarted",   // user re-enabled auto-renew
  12: "subscription_revoked",    // refunded / revoked
  13: "subscription_expired",
};

export const playRtdnHandler = onMessagePublished(
  {region: REGION, maxInstances: DEFAULT_MAX_INSTANCES, topic: PLAY_RTDN_TOPIC},
  async (event) => {
    const dataB64 = event.data.message.data;
    if (!dataB64) return;

    let payload: DeveloperNotification;
    try {
      payload = JSON.parse(Buffer.from(dataB64, "base64").toString("utf8"));
    } catch {
      // eslint-disable-next-line no-console
      console.warn("RTDN: unparseable message");
      return;
    }

    if (payload.testNotification) {
      // eslint-disable-next-line no-console
      console.log("RTDN: test notification received — topic wired correctly.");
      return;
    }

    const sub = payload.subscriptionNotification;
    if (!sub?.purchaseToken) return; // not a subscription event we handle

    // Map the purchaseToken → uid via the ledger written by verifyPurchase.
    const ledger = await db
      .collection(Collections.processedPurchases)
      .doc(sub.purchaseToken)
      .get();
    if (!ledger.exists) {
      // eslint-disable-next-line no-console
      console.warn("RTDN: no user mapped to this purchaseToken yet.");
      return;
    }
    const uid = ledger.data()?.uid as string | undefined;
    if (!uid) return;

    // Analytics: renewals / cancellations etc. happen only here, server-side.
    const eventName = RTDN_EVENT_NAMES[sub.notificationType];
    if (eventName) {
      await logServerEvent(eventName, uid, ledger.data()?.appInstanceId, {
        product_id: sub.subscriptionId,
        notification_type: sub.notificationType,
      });
    }

    // Authoritative re-check against the Play API (never trust the payload).
    // fetchSubscriptionState keeps a CANCELED-but-not-yet-expired subscription
    // ACTIVE, so cancelling auto-renew does NOT revoke premium early.
    const {active, expiresAt} = await fetchSubscriptionState(sub.purchaseToken);
    await setSubscriptionState(uid, active, expiresAt);
    // eslint-disable-next-line no-console
    console.log(`RTDN: ${uid} subscriptionActive=${active}`);
  }
);
