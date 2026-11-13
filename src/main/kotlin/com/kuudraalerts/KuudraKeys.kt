package com.kuudraalerts

// Key shop prices from the Mage/Barbarian Emissary (hypixelskyblock.minecraft.wiki/w/Kuudra_Keys), before reputation discounts.
enum class KuudraTier(val label: String, val color: Int, val materialPerKey: Int, val coinsPerKey: Long) {
    BASIC("Basic", 0xAAAAAA, 2, 200_000),
    HOT("Hot", 0xFFFF55, 6, 400_000),
    BURNING("Burning", 0xFFAA00, 20, 750_000),
    FIERY("Fiery", 0xFF5555, 60, 1_500_000),
    INFERNAL("Infernal", 0xAA00AA, 120, 3_000_000);

    val keyName = if (label == "Basic") "Kuudra Key" else "$label Kuudra Key"

    companion object {
        const val STARS_PER_KEY = 2
        private val TIER_WORD = Regex("""\b(Basic|Hot|Burning|Fiery|Infernal)\b""")

        /** Tier of a run from its item lore; a bare "Kuudra Key" mention means Basic. */
        fun fromLore(lines: List<String>): KuudraTier? =
            lines.firstNotNullOfOrNull { line -> TIER_WORD.find(line)?.let { m -> entries.first { it.label == m.value } } }
                ?: BASIC.takeIf { lines.any { "Kuudra Key" in it } }
    }
}

enum class Faction(val label: String, val keyMaterial: String) {
    MAGE("Mage", "Enchanted Mycelium"),
    BARBARIAN("Barbarian", "Enchanted Red Sand");

    companion object {
        // Matches tab lines such as "Mage Reputation: 1,234" or "Faction: Barbarian"; ignores "Mage Outlaw" etc.
        private val FACTION_LINE = Regex("""\b(Mage|Barbarian)\b.*\bRep|\bFaction\b.*\b(Mage|Barbarian)\b""")

        fun fromTabLines(lines: List<String>): Faction? = lines.firstNotNullOfOrNull { line ->
            FACTION_LINE.find(line)?.let { m -> if ((m.groups[1] ?: m.groups[2])!!.value == "Mage") MAGE else BARBARIAN }
        }
    }
}

class ShoppingList(val keys: Map<KuudraTier, Int>, val material: Int, val stars: Int, val coins: Long)

/** One key per unopened paid chest, minus keys and materials already in the inventory. */
fun shoppingList(unopened: Map<KuudraTier, Int>, ownedKeys: Map<KuudraTier, Int>, ownedMaterial: Int, ownedStars: Int): ShoppingList {
    val keys = KuudraTier.entries
        .associateWith { maxOf(0, (unopened[it] ?: 0) - (ownedKeys[it] ?: 0)) }
        .filterValues { it > 0 }
    return ShoppingList(
        keys = keys,
        material = maxOf(0, keys.entries.sumOf { (t, n) -> t.materialPerKey * n } - ownedMaterial),
        stars = maxOf(0, keys.values.sum() * KuudraTier.STARS_PER_KEY - ownedStars),
        coins = keys.entries.sumOf { (t, n) -> t.coinsPerKey * n },
    )
}
