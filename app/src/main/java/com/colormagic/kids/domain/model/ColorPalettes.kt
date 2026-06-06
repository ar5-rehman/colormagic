package com.colormagic.kids.domain.model

// A named set of swatches the kid can switch between (Classic, Pastel, Neon…).
// Lets kids paint in a whole different "mood" without a custom color picker.
data class NamedPalette(
    val id: String,
    val name: String,
    val colors: List<PaintColor>
)

object ColorPalettes {
    private val classic = NamedPalette("classic", "Classic", DefaultPalette.colors)

    private val pastel = NamedPalette(
        "pastel", "Pastel",
        listOf(
            PaintColor("p_red", "Blush", 0xFFFFB3BA),
            PaintColor("p_orange", "Peach", 0xFFFFD9B3),
            PaintColor("p_yellow", "Butter", 0xFFFFF5BA),
            PaintColor("p_green", "Mint", 0xFFBAFFC9),
            PaintColor("p_teal", "Seafoam", 0xFFBAF7E8),
            PaintColor("p_sky", "Baby Blue", 0xFFBAE1FF),
            PaintColor("p_blue", "Periwinkle", 0xFFC9C9FF),
            PaintColor("p_violet", "Lilac", 0xFFE0C3FC),
            PaintColor("p_pink", "Cotton Candy", 0xFFFFC8E1),
            PaintColor("p_brown", "Sand", 0xFFE8D8C3),
            PaintColor("p_grey", "Cloud", 0xFFE6E6EA),
            PaintColor("p_black", "Slate", 0xFF8E8E9C)
        )
    )

    private val neon = NamedPalette(
        "neon", "Neon",
        listOf(
            PaintColor("n_red", "Hot Pink", 0xFFFF1F6B),
            PaintColor("n_orange", "Tangerine", 0xFFFF6D00),
            PaintColor("n_yellow", "Laser", 0xFFFFEA00),
            PaintColor("n_lime", "Slime", 0xFFAEFF00),
            PaintColor("n_green", "Acid Green", 0xFF00E676),
            PaintColor("n_teal", "Aqua", 0xFF00E5FF),
            PaintColor("n_blue", "Electric", 0xFF2979FF),
            PaintColor("n_violet", "Ultra", 0xFF7C4DFF),
            PaintColor("n_magenta", "Magenta", 0xFFE500FF),
            PaintColor("n_white", "Glow", 0xFFFFFFFF),
            PaintColor("n_black", "Blacklight", 0xFF1A0033)
        )
    )

    private val ocean = NamedPalette(
        "ocean", "Ocean",
        listOf(
            PaintColor("o_foam", "Foam", 0xFFE0F7FA),
            PaintColor("o_sky", "Sky", 0xFF81D4FA),
            PaintColor("o_lagoon", "Lagoon", 0xFF26C6DA),
            PaintColor("o_teal", "Teal", 0xFF00ACC1),
            PaintColor("o_sea", "Sea", 0xFF0097A7),
            PaintColor("o_deep", "Deep Blue", 0xFF1565C0),
            PaintColor("o_navy", "Navy", 0xFF0D3B66),
            PaintColor("o_kelp", "Kelp", 0xFF2E7D32),
            PaintColor("o_coral", "Coral", 0xFFFF7043),
            PaintColor("o_sand", "Sand", 0xFFFFE0B2),
            PaintColor("o_pearl", "Pearl", 0xFFFFF8E1)
        )
    )

    /** All palettes, in display order. First is the default. */
    val all: List<NamedPalette> = listOf(classic, pastel, neon, ocean)

    val default: NamedPalette get() = all.first()

    fun byId(id: String): NamedPalette = all.firstOrNull { it.id == id } ?: default
}
