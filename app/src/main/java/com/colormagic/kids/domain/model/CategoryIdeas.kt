package com.colormagic.kids.domain.model

// Shared catalogue of kid-friendly prompts grouped by category.
//
// Used by:
//   • CreateSketch — tapping a category chip prefills the prompt with a random
//                    idea from that category's pool.
//   • Home         — tapping a category card on Home navigates to CreateSketch
//                    and prefills similarly (via a nav argument).
//   • Gallery      — every saved artwork tags itself with one of these
//                    categories so the gallery filter can group them.
//
// Keep the keys lowercase + url-safe (they travel as nav arguments).
object CategoryIdeas {

    /** Stable category keys — used in nav routes and Firestore docs. */
    const val ANIMALS = "animals"
    const val SPACE = "space"
    const val DINOSAURS = "dinosaurs"
    const val ROBOTS = "robots"
    const val PRINCESS = "princess"
    const val NATURE = "nature"
    const val VEHICLES = "vehicles"
    const val MAGIC = "magic"

    /** Display label for a key. Keep the key→label mapping in one place. */
    val labels: Map<String, String> = mapOf(
        ANIMALS to "Animals",
        SPACE to "Space",
        DINOSAURS to "Dinosaurs",
        ROBOTS to "Robots",
        PRINCESS to "Princess",
        NATURE to "Nature",
        VEHICLES to "Vehicles",
        MAGIC to "Magic"
    )

    /** Playful emoji per category — used as the idea card's "illustration"
     *  (and on category chips) while there are no preview images. */
    val emoji: Map<String, String> = mapOf(
        ANIMALS to "🐘",
        SPACE to "🚀",
        DINOSAURS to "🦕",
        ROBOTS to "🤖",
        PRINCESS to "👑",
        NATURE to "🌻",
        VEHICLES to "🚒",
        MAGIC to "🦄"
    )

    /** Distinct pastel background per category so the idea cards feel colorful
     *  and varied rather than all one tint. */
    val tint: Map<String, Long> = mapOf(
        ANIMALS to 0xFFFFF3E0,    // peach
        SPACE to 0xFFE3F2FD,      // sky blue
        DINOSAURS to 0xFFE8F5E9,  // mint
        ROBOTS to 0xFFE0F7FA,     // cyan
        PRINCESS to 0xFFFCE4EC,   // pink
        NATURE to 0xFFF1F8E9,     // light green
        VEHICLES to 0xFFFFF8E1,   // soft yellow
        MAGIC to 0xFFEDE7F6       // lavender
    )

    private val pools: Map<String, List<String>> = mapOf(
        ANIMALS to listOf(
            "A friendly elephant holding a balloon",
            "A sleepy puppy in a basket",
            "A penguin family ice-skating",
            "A koala hugging a tree branch",
            "A baby giraffe with a bowtie",
            "A bunny eating a giant carrot",
            "A fluffy kitten playing with a ball of yarn",
            "A baby panda munching on bamboo",
            "A wise owl wearing tiny round glasses",
            "A happy turtle with a flower on its shell",
            "A little fox curled up under a leaf",
            "A duckling splashing in a puddle"
        ),
        SPACE to listOf(
            "A happy rocket flying past smiling planets",
            "An astronaut waving from the moon",
            "A friendly alien playing with a star",
            "A space cat in a tiny spaceship",
            "A planet wearing sunglasses",
            "A smiling star riding a comet",
            "A little rover exploring a bumpy planet",
            "An astronaut puppy floating in space",
            "A swirly galaxy full of cute little stars"
        ),
        DINOSAURS to listOf(
            "A cute baby T-Rex eating an apple",
            "A friendly long-neck dinosaur reaching for leaves",
            "A small triceratops smiling in the grass",
            "A baby dinosaur hatching from an egg",
            "A stegosaurus with a flower crown",
            "A pteranodon gliding over green hills",
            "A happy dino family having a picnic",
            "A baby raptor chasing a butterfly"
        ),
        ROBOTS to listOf(
            "A round robot waving hello",
            "A robot pet dog wagging its tail",
            "A friendly robot watering plants",
            "A robot chef baking a cupcake",
            "A tiny robot riding a skateboard",
            "A robot painting a big rainbow",
            "A helper robot carrying a stack of books",
            "A dancing robot with light-up feet"
        ),
        PRINCESS to listOf(
            "A princess with a big bow petting a kitten",
            "A young prince waving from a castle window",
            "A friendly fairy holding a wand",
            "A princess riding a unicorn",
            "A castle with hearts on the flags",
            "A princess having a tea party with teddy bears",
            "A brave prince and a friendly dragon",
            "A fairy sprinkling sparkles over flowers"
        ),
        NATURE to listOf(
            "A smiling sunflower in a meadow",
            "A friendly tree with eyes and a smile",
            "A happy mushroom under a leaf umbrella",
            "A butterfly resting on a daisy",
            "A rainbow over a tiny pond with fish",
            "A cheerful cloud raining little hearts",
            "A ladybug sitting on a big green leaf",
            "A garden of giggling flowers"
        ),
        VEHICLES to listOf(
            "A bright fire truck with a smile",
            "A friendly school bus with stars",
            "A small sailboat with a flag",
            "A train with smiley face windows",
            "A hot air balloon with cute clouds",
            "A race car zooming with a big grin",
            "A digger truck scooping up sand",
            "A little airplane looping in the sky"
        ),
        MAGIC to listOf(
            "A fluffy unicorn eating a strawberry cupcake",
            "A baby dragon blowing tiny bubbles",
            "A mermaid waving from a seashell",
            "A wizard cat with a starry hat",
            "A friendly genie popping out of a teapot",
            "A unicorn sliding down a rainbow",
            "A tiny fairy riding a snail",
            "A magic owl with glowing feathers"
        )
    )

    /** Flat list of every idea — used as the "Need ideas?" rotating pool. */
    val allIdeas: List<String> = pools.values.flatten()

    /** One idea + the category it belongs to (so the UI can pick the matching
     *  emoji + color for its card). */
    data class IdeaItem(val text: String, val category: String)

    /** Every idea tagged with its category — the "Need ideas?" pool the UI
     *  shuffles, so each card gets a relevant emoji + color. */
    val allIdeaItems: List<IdeaItem> = pools.flatMap { (category, list) ->
        list.map { IdeaItem(it, category) }
    }

    /** Returns a random prompt from [category]'s pool, or null if unknown. */
    fun randomIdeaFor(category: String): String? =
        pools[category]?.randomOrNull()

    /** All known category keys, stable order. */
    val keys: List<String> = labels.keys.toList()
}
