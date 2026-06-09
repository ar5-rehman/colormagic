package com.colormagic.kids.domain.model

/**
 * "Today's magic word" — a fresh suggested idea each day. The pick is
 * deterministic per local day, so it's the same every time the app opens that
 * day and changes at midnight, giving kids a gentle reason to come back.
 */
object DailyTheme {

    private val ideas: List<String> = listOf(
        "a dancing dinosaur",
        "a rocket flying to the moon",
        "a friendly dragon chef",
        "a cat astronaut",
        "a magical unicorn castle",
        "a robot playing soccer",
        "a happy elephant with balloons",
        "an underwater mermaid city",
        "a superhero puppy",
        "a giant ice-cream mountain",
        "a fairy in a flower garden",
        "a pirate ship in the clouds",
        "a dinosaur birthday party",
        "a penguin on a snowy slide",
        "a wizard owl with a hat",
        "a race car made of candy",
        "a bunny baking cupcakes",
        "a friendly monster under the bed",
        "a turtle with a rainbow shell",
        "a train full of zoo animals",
        "a butterfly with star wings",
        "a snowman going surfing",
        "a lion king on a jungle throne",
        "a spaceship picnic on Mars",
        "a koala riding a skateboard",
        "a whale jumping over a rainbow",
        "a tiny fairy house in a mushroom",
        "a dog and cat best-friend band",
        "a castle made of jelly beans",
        "a friendly shark with sunglasses"
    )

    /** The idea for the current local day. */
    fun todaysIdea(): String {
        val day = (System.currentTimeMillis() / DAY_MS).toInt()
        val index = ((day % ideas.size) + ideas.size) % ideas.size
        return ideas[index]
    }

    private const val DAY_MS = 24L * 60L * 60L * 1000L
}
