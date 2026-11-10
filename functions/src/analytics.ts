/**
 * Server-side analytics for events the app never sees — Play subscription
 * renewals and cancellations arrive only as RTDN messages on the server.
 *
 *  • Always: a row in Firestore `subscriptionEvents` (your own audit trail /
 *    reporting, also handy in BigQuery exports).
 *  • When configured: the same event in Firebase Analytics via the GA4
 *    Measurement Protocol, attributed to the purchasing app instance (the app
 *    sends its Analytics appInstanceId with every purchase; verifyPurchase
 *    stores it on the purchase ledger).
 *
 * Setup for Firebase Analytics delivery (one-time): Firebase Console → Project
 * settings → Integrations → Google Analytics → open GA4 → Admin → Data streams
 * → the Android stream → Measurement Protocol API secrets → Create. Put the
 * value in functions/.env as GA4_API_SECRET=... and redeploy.
 */
import {Timestamp} from "firebase-admin/firestore";
import {FIREBASE_ANDROID_APP_ID} from "./config";
import {db} from "./firebase";

export async function logServerEvent(
  name: string,
  uid: string,
  appInstanceId: string | null | undefined,
  params: Record<string, string | number>
): Promise<void> {
  await db.collection("subscriptionEvents").add({
    name,
    uid,
    ...params,
    createdAt: Timestamp.now(),
  }).catch((err) => console.warn("subscriptionEvents write failed", err));

  const secret = process.env.GA4_API_SECRET?.trim();
  if (!secret || !appInstanceId) return;
  const url = "https://www.google-analytics.com/mp/collect" +
    `?firebase_app_id=${encodeURIComponent(FIREBASE_ANDROID_APP_ID)}` +
    `&api_secret=${encodeURIComponent(secret)}`;
  await fetch(url, {
    method: "POST",
    headers: {"Content-Type": "application/json"},
    body: JSON.stringify({
      app_instance_id: appInstanceId,
      user_id: uid,
      events: [{name, params}],
    }),
  }).catch((err) => console.warn(`Analytics MP send failed for ${name}`, err));
}
