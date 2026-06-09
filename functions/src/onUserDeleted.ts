/**
 * Auth trigger: onUserDeleted
 *
 * When a Firebase Auth user is deleted, remove its Firestore users/{uid} doc
 * (and is harmless if there isn't one). The app deletes throwaway anonymous
 * "guest" accounts on logout→login when it falls back to an existing Google
 * account; this trigger keeps the `users` collection from filling up with
 * orphaned guest docs to match.
 *
 * Note: v1 Auth triggers run in us-central1 (region cannot be changed), which
 * is independent of the callable functions' REGION.
 */
import * as functionsV1 from "firebase-functions/v1";
import {db, Collections} from "./firebase";

export const onUserDeleted = functionsV1.auth.user().onDelete(async (user) => {
  await db
    .collection(Collections.users)
    .doc(user.uid)
    .delete()
    .catch((err) => {
      // eslint-disable-next-line no-console
      console.error(`onUserDeleted: failed to delete users/${user.uid}`, err);
    });
});
