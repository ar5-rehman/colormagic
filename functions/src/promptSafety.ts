/**
 * Two-layer prompt safety gate for a 4–10 year-old audience.
 *
 *  Layer 1 — keyword blocklist (this file): deterministic, instant, free.
 *            Catches the kid-app-specific risks a generic model misses —
 *            real people, celebrities, copyrighted characters, brands.
 *  Layer 2 — OpenAI moderation endpoint (moderatePrompt): catches the
 *            broad harm categories (violence, sexual, self-harm, hate…).
 *
 * Both run before any image model is called. A prompt must clear BOTH.
 */
import type OpenAI from "openai";

export interface SafetyResult {
  safe: boolean;
  /** Internal reason for logs — never shown to the child. */
  reason?: string;
}

/** Kid-friendly message shown when a prompt is rejected. */
export const UNSAFE_PROMPT_MESSAGE = "Please try a safe and kid-friendly idea.";

/**
 * Lower-cased substrings that immediately fail a prompt. Grouped by the
 * category they defend. Intentionally broad — false positives are far
 * cheaper than a bad image in front of a child.
 */
const BLOCKLIST: Record<string, string[]> = {
  violence: ["kill", "gun", "shoot", "blood", "weapon", "knife", "war", "fight", "punch", "explode", "bomb"],
  scary: ["scary", "horror", "nightmare", "demon", "zombie", "ghost", "creepy", "monster killing"],
  adult: ["sexy", "nude", "naked", "kiss", "romantic", "boyfriend", "girlfriend", "dating"],
  selfHarm: ["suicide", "self harm", "self-harm", "cutting", "hurt myself"],
  hate: ["racist", "nazi", "hate"],
  ipAndPeople: [
    "elsa", "mickey", "pokemon", "pikachu", "spiderman", "spider-man",
    "batman", "superman", "disney", "marvel", "barbie", "sonic",
    "celebrity", "president", "taylor swift", "ronaldo", "messi",
  ],
  personalInfo: ["my address", "phone number", "my school", "home address"],
  dangerous: ["fire", "matches", "poison", "drugs", "alcohol", "cigarette"],
};

/** Layer 1 — keyword scan. */
export function keywordCheck(prompt: string): SafetyResult {
  const text = prompt.toLowerCase();
  for (const [category, words] of Object.entries(BLOCKLIST)) {
    for (const word of words) {
      if (text.includes(word)) {
        return {safe: false, reason: `blocklist:${category}:${word}`};
      }
    }
  }
  return {safe: true};
}

/**
 * Layer 2 — OpenAI moderation. Free endpoint; flags the broad harm
 * categories. If the API call itself fails we FAIL CLOSED (treat as unsafe)
 * rather than letting an unvetted prompt through.
 */
export async function moderatePrompt(
  openai: OpenAI,
  prompt: string
): Promise<SafetyResult> {
  try {
    const result = await openai.moderations.create({input: prompt});
    const flagged = result.results.some((r) => r.flagged);
    return flagged ? {safe: false, reason: "moderation:flagged"} : {safe: true};
  } catch (err) {
    // eslint-disable-next-line no-console
    console.error("Moderation API failed — failing closed", err);
    return {safe: false, reason: "moderation:error"};
  }
}

/** Runs both layers. Returns the first failure, or safe. */
export async function checkPromptSafety(
  openai: OpenAI,
  prompt: string
): Promise<SafetyResult> {
  const layer1 = keywordCheck(prompt);
  if (!layer1.safe) return layer1;
  return moderatePrompt(openai, prompt);
}
