package com.colormagic.kids.domain.model

import java.util.Calendar

data class DailyChallenge(
    val prompt: String,
    val emoji: String,
    val theme: String
)

object DailyChallengeProvider {

    private val challenges = listOf(
        DailyChallenge("A happy rainbow over a field of flowers", "🌈", "Rainbow Day"),
        DailyChallenge("A friendly dragon baking cookies", "🐉", "Dragon Day"),
        DailyChallenge("An underwater castle with mermaids", "🧜‍♀️", "Ocean Day"),
        DailyChallenge("A rocket ship landing on the moon", "🚀", "Space Day"),
        DailyChallenge("A teddy bear having a picnic", "🧸", "Picnic Day"),
        DailyChallenge("A magical forest with talking animals", "🌲", "Forest Day"),
        DailyChallenge("A superhero puppy saving the day", "🦸", "Hero Day"),
        DailyChallenge("A birthday cake with lots of candles", "🎂", "Birthday Day"),
        DailyChallenge("A pirate treasure map on an island", "🏴‍☠️", "Pirate Day"),
        DailyChallenge("A fairy princess in a flower garden", "🧚", "Fairy Day"),
        DailyChallenge("A dinosaur playing soccer", "🦕", "Dino Day"),
        DailyChallenge("A cozy treehouse in the clouds", "🏡", "Treehouse Day"),
        DailyChallenge("A friendly robot building a sandcastle", "🤖", "Robot Day"),
        DailyChallenge("A unicorn galloping through a rainbow", "🦄", "Unicorn Day"),
        DailyChallenge("A cat astronaut floating in space", "🐱", "Cat Day"),
        DailyChallenge("A magical ice cream truck", "🍦", "Ice Cream Day"),
        DailyChallenge("A penguin sliding down a snowy hill", "🐧", "Penguin Day"),
        DailyChallenge("A butterfly garden with sparkles", "🦋", "Butterfly Day"),
        DailyChallenge("An enchanted castle with towers", "🏰", "Castle Day"),
        DailyChallenge("A whale singing to fish friends", "🐋", "Whale Day"),
        DailyChallenge("A jungle explorer with a parrot", "🦜", "Jungle Day"),
        DailyChallenge("A magical potion workshop", "🧪", "Potion Day"),
        DailyChallenge("A sleeping baby owl on a branch", "🦉", "Owl Day"),
        DailyChallenge("A candy land with chocolate rivers", "🍬", "Candy Day"),
        DailyChallenge("A ladybug family on a leaf", "🐞", "Bug Day"),
        DailyChallenge("A racing car made of fruits", "🏎️", "Race Day"),
        DailyChallenge("A giraffe reaching for the stars", "🦒", "Giraffe Day"),
        DailyChallenge("A magical music band of animals", "🎵", "Music Day"),
        DailyChallenge("A friendly alien visiting Earth", "👽", "Alien Day"),
        DailyChallenge("A cozy winter cabin with snow", "❄️", "Winter Day"),
        DailyChallenge("A tropical beach sunset", "🌅", "Beach Day")
    )

    fun today(): DailyChallenge {
        val cal = Calendar.getInstance()
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val year = cal.get(Calendar.YEAR)
        val index = ((dayOfYear + year) % challenges.size)
        return challenges[index]
    }
}
