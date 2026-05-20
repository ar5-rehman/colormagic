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

    private val pools: Map<String, List<String>> = mapOf(
        ANIMALS to listOf(
            "A friendly elephant holding a balloon",
            "A sleepy puppy in a basket",
            "A penguin family ice-skating",
            "A koala hugging a tree branch",
            "A baby giraffe with a bowtie",
            "A bunny eating a giant carrot"
        ),
        SPACE to listOf(
            "A happy rocket flying past smiling planets",
            "An astronaut waving from the moon",
            "A friendly alien playing with a star",
            "A space cat in a tiny spaceship",
            "A planet wearing sunglasses"
        ),
        DINOSAURS to listOf(
            "A cute baby T-Rex eating an apple",
            "A friendly long-neck dinosaur reaching for leaves",
            "A small triceratops smiling in the grass",
            "A baby dinosaur hatching from an egg",
            "A stegosaurus with a flower crown"
        ),
        ROBOTS to listOf(
            "A round robot waving hello",
            "A robot pet dog wagging its tail",
            "A friendly robot watering plants",
            "A robot chef baking a cupcake",
            "A tiny robot riding a skateboard"
        ),
        PRINCESS to listOf(
            "A princess with a big bow petting a kitten",
            "A young prince waving from a castle window",
            "A friendly fairy holding a wand",
            "A princess riding a unicorn",
            "A castle with hearts on the flags"
        ),
        NATURE to listOf(
            "A smiling sunflower in a meadow",
            "A friendly tree with eyes and a smile",
            "A happy mushroom under a leaf umbrella",
            "A butterfly resting on a daisy",
            "A rainbow over a tiny pond with fish"
        ),
        VEHICLES to listOf(
            "A bright fire truck with a smile",
            "A friendly school bus with stars",
            "A small sailboat with a flag",
            "A train with smiley face windows",
            "A hot air balloon with cute clouds"
        ),
        MAGIC to listOf(
            "A fluffy unicorn eating a strawberry cupcake",
            "A baby dragon blowing tiny bubbles",
            "A mermaid waving from a seashell",
            "A wizard cat with a starry hat",
            "A friendly genie popping out of a teapot"
        )
    )

    /** Flat list of every idea — used as the "Need ideas?" rotating pool. */
    val allIdeas: List<String> = pools.values.flatten()

    /** Returns a random prompt from [category]'s pool, or null if unknown. */
    fun randomIdeaFor(category: String): String? =
        pools[category]?.randomOrNull()

    /** All known category keys, stable order. */
    val keys: List<String> = labels.keys.toList()
}
