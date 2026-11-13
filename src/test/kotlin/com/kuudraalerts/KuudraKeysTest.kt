package com.kuudraalerts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KuudraKeysTest {
    @Test
    fun tierFromLore() {
        assertEquals(KuudraTier.INFERNAL, KuudraTier.fromLore(listOf("Tier: Infernal", "Click to view!")))
        assertEquals(KuudraTier.HOT, KuudraTier.fromLore(listOf("Requires Hot Kuudra Key")))
        assertEquals(KuudraTier.BASIC, KuudraTier.fromLore(listOf("Requires Kuudra Key")))
        assertNull(KuudraTier.fromLore(listOf("Hotbar", "Click to open!")))
    }

    @Test
    fun factionFromTab() {
        assertEquals(Faction.MAGE, Faction.fromTabLines(listOf("Mage Outlaw: Alive", "Mage Reputation: 12,000")))
        assertEquals(Faction.BARBARIAN, Faction.fromTabLines(listOf("Faction: Barbarian")))
        assertNull(Faction.fromTabLines(listOf("Mage Outlaw: Alive", "Barbarian Duke X: Dead")))
    }

    @Test
    fun shoppingSubtractsOwned() {
        val list = shoppingList(
            unopened = mapOf(KuudraTier.INFERNAL to 3, KuudraTier.BURNING to 1),
            ownedKeys = mapOf(KuudraTier.INFERNAL to 1),
            ownedMaterial = 40,
            ownedStars = 1,
        )
        assertEquals(mapOf(KuudraTier.INFERNAL to 2, KuudraTier.BURNING to 1), list.keys)
        assertEquals(2 * 120 + 20 - 40, list.material)
        assertEquals(3 * 2 - 1, list.stars)
        assertEquals(2 * 3_000_000L + 750_000L, list.coins)
    }
}
