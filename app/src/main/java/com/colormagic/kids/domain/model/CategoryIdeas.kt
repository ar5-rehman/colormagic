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

    /** Keywords per category, checked in this order so the more specific
     *  category wins a tie (a "robot dog" is Robots, a "space cat" is Space). */
    private val keywords: List<Pair<String, Set<String>>> = listOf(
        DINOSAURS to setOf(
            "dinosaur", "dino", "rex", "trex", "triceratops", "stegosaurus",
            "pteranodon", "raptor", "brontosaurus", "brachiosaurus", "velociraptor"
        ),
        ROBOTS to setOf("robot", "robo", "android", "cyborg", "machine"),
        SPACE to setOf(
            "space", "rocket", "astronaut", "alien", "planet", "moon", "star",
            "comet", "galaxy", "rover", "spaceship", "ufo", "satellite", "saturn", "mars"
        ),
        VEHICLES to setOf(
            "car", "truck", "bus", "train", "boat", "sailboat", "ship", "plane",
            "airplane", "helicopter", "tractor", "digger", "bike", "bicycle",
            "motorcycle", "submarine", "balloon", "taxi", "ambulance", "scooter"
        ),
        PRINCESS to setOf(
            "princess", "prince", "queen", "king", "castle", "crown", "tiara",
            "knight", "palace", "royal"
        ),
        MAGIC to setOf(
            "unicorn", "dragon", "mermaid", "wizard", "witch", "genie", "fairy",
            "magic", "magical", "wand", "spell", "pegasus", "phoenix", "elf", "gnome"
        ),
        ANIMALS to setOf(
            "animal", "elephant", "puppy", "dog", "cat", "kitten", "penguin",
            "koala", "giraffe", "bunny", "rabbit", "panda", "owl", "turtle",
            "fox", "duck", "duckling", "lion", "tiger", "bear", "monkey", "horse",
            "pony", "cow", "pig", "sheep", "goat", "chicken", "bird", "fish",
            "whale", "dolphin", "shark", "octopus", "frog", "mouse", "hamster",
            "zebra", "hippo", "crocodile", "snake", "deer", "squirrel", "hedgehog",
            "sloth", "otter", "seal", "parrot", "flamingo", "bee", "snail"
        ),
        NATURE to setOf(
            "flower", "sunflower", "daisy", "rose", "tree", "forest", "garden",
            "meadow", "mushroom", "leaf", "rainbow", "cloud", "rain", "sun",
            "pond", "river", "mountain", "beach", "ocean", "butterfly", "ladybug",
            "nature", "park", "jungle", "volcano", "waterfall"
        )
    )

    /** Ideas from the built-in pools map straight to their own category. */
    private val ideaCategory: Map<String, String> =
        allIdeaItems.associate { it.text.lowercase() to it.category }

    /**
     * Best-guess category key for a prompt (or a title derived from one), or
     * null when nothing matches — such artworks then appear only under "All".
     */
    fun categorize(prompt: String?): String? {
        val text = prompt?.trim()?.lowercase().orEmpty()
        if (text.isEmpty()) return null
        ideaCategory[text]?.let { return it }

        val words = text.split(Regex("[^a-z]+"))
            .filter { it.isNotEmpty() }
            .map {
                when {
                    it.length > 4 && it.endsWith("ies") -> it.dropLast(3) + "y" // puppies → puppy
                    it.length > 3 && it.endsWith("s") -> it.dropLast(1)          // trucks → truck
                    else -> it
                }
            }
            .toSet() + text.replace("-", "").split(Regex("[^a-z]+"))
        // Highest keyword count wins; list order breaks ties.
        return keywords
            .map { (key, set) -> key to words.count { it in set } }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }
            ?.first
    }
}
