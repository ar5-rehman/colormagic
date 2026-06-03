/**
 * ColorMagic Kids — Firebase Functions entry point.
 *
 * `./firebase` is imported first so the Admin SDK is initialized before any
 * callable touches Firestore / Storage.
 */
import "./firebase";

export {generateSketch} from "./generateSketch";
export {userQuota} from "./userQuota";
export {verifyPurchase} from "./verifyPurchase";
export {grantRewardedAdCredits} from "./grantRewardedAdCredits";
