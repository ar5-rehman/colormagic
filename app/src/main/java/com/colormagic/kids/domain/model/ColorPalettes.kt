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

    private val sunset = NamedPalette(
        "sunset", "Sunset",
        listOf(
            PaintColor("s_gold", "Gold", 0xFFFFD54F),
            PaintColor("s_amber", "Amber", 0xFFFFB300),
            PaintColor("s_tangerine", "Tangerine", 0xFFFF8F00),
            PaintColor("s_flame", "Flame", 0xFFFF6D00),
            PaintColor("s_ember", "Ember", 0xFFE65100),
            PaintColor("s_crimson", "Crimson", 0xFFD50000),
            PaintColor("s_rose", "Rose", 0xFFFF5252),
            PaintColor("s_blush", "Blush", 0xFFFF8A80),
            PaintColor("s_peach", "Peach", 0xFFFFCCBC),
            PaintColor("s_lavender", "Lavender", 0xFFCE93D8),
            PaintColor("s_plum", "Plum", 0xFF7B1FA2),
            PaintColor("s_midnight", "Midnight", 0xFF311B92)
        )
    )

    private val rainbow = NamedPalette(
        "rainbow", "Rainbow",
        listOf(
            PaintColor("rb_red", "Red", 0xFFFF1744),
            PaintColor("rb_orange", "Orange", 0xFFFF9100),
            PaintColor("rb_yellow", "Yellow", 0xFFFFEA00),
            PaintColor("rb_chartreuse", "Chartreuse", 0xFFC6FF00),
            PaintColor("rb_green", "Green", 0xFF00E676),
            PaintColor("rb_cyan", "Cyan", 0xFF00E5FF),
            PaintColor("rb_blue", "Blue", 0xFF2979FF),
            PaintColor("rb_indigo", "Indigo", 0xFF3D5AFE),
            PaintColor("rb_violet", "Violet", 0xFFD500F9),
            PaintColor("rb_pink", "Pink", 0xFFFF4081),
            PaintColor("rb_white", "White", 0xFFFFFFFF),
            PaintColor("rb_black", "Black", 0xFF212121)
        )
    )

    private val earth = NamedPalette(
        "earth", "Earth",
        listOf(
            PaintColor("e_cream", "Cream", 0xFFFFF8E1),
            PaintColor("e_wheat", "Wheat", 0xFFFFE0B2),
            PaintColor("e_caramel", "Caramel", 0xFFFFB74D),
            PaintColor("e_sienna", "Sienna", 0xFFA1887F),
            PaintColor("e_clay", "Clay", 0xFF8D6E63),
            PaintColor("e_bark", "Bark", 0xFF5D4037),
            PaintColor("e_moss", "Moss", 0xFF689F38),
            PaintColor("e_olive", "Olive", 0xFF827717),
            PaintColor("e_forest", "Forest", 0xFF33691E),
            PaintColor("e_stone", "Stone", 0xFF90A4AE),
            PaintColor("e_slate", "Slate", 0xFF546E7A),
            PaintColor("e_charcoal", "Charcoal", 0xFF37474F)
        )
    )

    private val candy = NamedPalette(
        "candy", "Candy",
        listOf(
            PaintColor("c_bubblegum", "Bubblegum", 0xFFFF80AB),
            PaintColor("c_strawberry", "Strawberry", 0xFFFF5252),
            PaintColor("c_lollipop", "Lollipop", 0xFFFF4081),
            PaintColor("c_grape", "Grape", 0xFFE040FB),
            PaintColor("c_blueberry", "Blueberry", 0xFF7C4DFF),
            PaintColor("c_jellybean", "Jellybean", 0xFF448AFF),
            PaintColor("c_sour_apple", "Sour Apple", 0xFF69F0AE),
            PaintColor("c_lemon", "Lemon Drop", 0xFFFFFF00),
            PaintColor("c_orange_drop", "Orange Drop", 0xFFFFAB40),
            PaintColor("c_cotton", "Cotton Candy", 0xFFF8BBD0),
            PaintColor("c_marshmallow", "Marshmallow", 0xFFFCE4EC),
            PaintColor("c_licorice", "Licorice", 0xFF212121)
        )
    )

    /** All palettes, in display order. First is the default. */
    val all: List<NamedPalette> = listOf(classic, pastel, neon, ocean, sunset, rainbow, earth, candy)

    val default: NamedPalette get() = all.first()

    fun byId(id: String): NamedPalette = all.firstOrNull { it.id == id } ?: default
}
