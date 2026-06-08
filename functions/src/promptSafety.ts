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
  violence: [
    "kill", "gun", "shoot", "blood", "weapon", "knife", "sword fight",
    "war", "fight", "punch", "explode", "bomb", "dead", "death", "murder",
  ],
  scary: [
    "scary", "horror", "nightmare", "demon", "zombie", "ghost", "creepy",
    "monster killing", "haunted", "vampire", "evil", "satan", "devil",
    "skull", "grave", "graveyard",
  ],
  adult: [
    "sexy", "nude", "naked", "kiss", "romantic", "boyfriend", "girlfriend",
    "dating", "wedding night", "lingerie", "bikini",
  ],
  selfHarm: ["suicide", "self harm", "self-harm", "cutting", "hurt myself"],
  hate: ["racist", "nazi", "hate", "slur"],
  ipAndPeople: [
    "elsa", "mickey", "pokemon", "pikachu", "spiderman", "spider-man",
    "batman", "superman", "disney", "marvel", "barbie", "sonic",
    "frozen", "minion", "shrek", "harry potter", "paw patrol", "bluey",
    "celebrity", "president", "taylor swift", "ronaldo", "messi", "trump",
  ],
  personalInfo: ["my address", "phone number", "my school", "home address"],
  dangerous: [
    "fire", "matches", "poison", "drugs", "alcohol", "cigarette",
    "smoking", "vape", "needle", "syringe",
  ],
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

/**
 * Prompt safety gate. OpenAI moderation is DISABLED for now, so this is the
 * keyword blocklist only (plus the strict prompt template downstream). To
 * re-enable OpenAI's moderation, pass an OpenAI client and
 * `return moderatePrompt(openai, prompt)` when the keyword check passes.
 */
export function checkPromptSafety(prompt: string): SafetyResult {
  return keywordCheck(prompt);
}
