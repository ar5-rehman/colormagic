package com.colormagic.kids.domain.model

data class StickerItem(
    val id: String,
    val emoji: String,
    val label: String
)

object StickerPack {
    val all: List<StickerItem> = listOf(
        StickerItem("star", "⭐", "Star"),
        StickerItem("heart", "❤️", "Heart"),
        StickerItem("rainbow", "🌈", "Rainbow"),
        StickerItem("sun", "☀️", "Sun"),
        StickerItem("moon", "🌙", "Moon"),
        StickerItem("flower", "🌸", "Flower"),
        StickerItem("butterfly", "🦋", "Butterfly"),
        StickerItem("crown", "👑", "Crown"),
        StickerItem("sparkles", "✨", "Sparkles"),
        StickerItem("cat", "🐱", "Cat"),
        StickerItem("dog", "🐶", "Dog"),
        StickerItem("unicorn", "🦄", "Unicorn"),
        StickerItem("rocket", "🚀", "Rocket"),
        StickerItem("diamond", "💎", "Diamond"),
        StickerItem("fire", "🔥", "Fire"),
        StickerItem("cloud", "☁️", "Cloud"),
        StickerItem("music", "🎵", "Music"),
        StickerItem("balloon", "🎈", "Balloon"),
        StickerItem("turtle", "🐢", "Turtle"),
        StickerItem("dino", "🦕", "Dino"),
        StickerItem("fish", "🐠", "Fish"),
        StickerItem("ladybug", "🐞", "Ladybug"),
        StickerItem("tree", "🌳", "Tree"),
        StickerItem("cake", "🎂", "Cake")
    )
}
