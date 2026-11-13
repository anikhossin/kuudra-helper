package com.kuudraalerts

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import java.util.EnumMap

/** Side panel on the Vesuvius menu: runs per tier, unopened chests, and the keys/materials to buy. */
object VesuviusOverlay {

    private const val RUN_ITEM = "Kuudra's Hollow"
    private const val CLAIMED_LINE = "No more chests to open!"
    private const val CHEST_WIDTH = 176
    private const val LINE_H = 10
    private const val PAD = 6

    // Last faction seen in the tab list; kept so the panel still works if the tab widget is hidden.
    private var faction: Faction? = null

    fun register() {
        // Fabric recreates per-screen events on every init, so this never stacks handlers or outlives the screen.
        ScreenEvents.AFTER_INIT.register { client, screen, _, _ ->
            if (screen !is ContainerScreen || "Vesuvius" !in screen.title.string) return@register
            faction = Faction.fromTabLines(client.connection?.listedOnlinePlayers.orEmpty().mapNotNull { it.tabListDisplayName?.string?.let(::strip) }) ?: faction
            ScreenEvents.afterExtract(screen).register { _, graphics, _, _, _ -> render(client, screen, graphics) }
        }
    }

    private fun strip(s: String) = ChatFormatting.stripFormatting(s) ?: s
    private fun name(stack: ItemStack) = strip(stack.hoverName.string)
    private fun lore(stack: ItemStack) = stack.get(DataComponents.LORE)?.lines()?.map { strip(it.string) }.orEmpty()

    private fun render(client: Minecraft, screen: ContainerScreen, g: GuiGraphicsExtractor) {
        val menu = screen.menu
        val left = (screen.width - CHEST_WIDTH) / 2
        val top = (screen.height - (114 + menu.rowCount * 18)) / 2

        val runs = EnumMap<KuudraTier, Int>(KuudraTier::class.java)
        val unopened = EnumMap<KuudraTier, Int>(KuudraTier::class.java)
        var unknownUnopened = 0
        for (slot in menu.slots.take(menu.rowCount * 9)) {
            val stack = slot.item
            if (name(stack) != RUN_ITEM) continue
            val lines = lore(stack)
            val open = CLAIMED_LINE !in lines
            val tier = KuudraTier.fromLore(lines)
            g.fill(left + slot.x, top + slot.y, left + slot.x + 16, top + slot.y + 16, if (open) 0x6055FF55 else 0x90000000.toInt())
            if (tier == null) { if (open) unknownUnopened++; continue }
            runs.merge(tier, 1, Int::plus)
            if (open) unopened.merge(tier, 1, Int::plus)
        }
        if (runs.isEmpty() && unknownUnopened == 0) return

        val owned = HashMap<String, Int>()
        client.player?.inventory?.nonEquipmentItems?.forEach { if (!it.isEmpty) owned.merge(name(it), it.count, Int::plus) }
        val f = faction
        val list = shoppingList(
            unopened,
            KuudraTier.entries.associateWith { owned[it.keyName] ?: 0 },
            f?.let { owned[it.keyMaterial] } ?: 0,
            owned["Nether Star"] ?: 0,
        )

        val lines = buildList {
            add("Kuudra Chests" to 0xFFAA00)
            for (t in KuudraTier.entries) runs[t]?.let { add("${t.label}: ${unopened[t] ?: 0} unopened / $it runs" to t.color) }
            if (unknownUnopened > 0) add("Unknown tier: $unknownUnopened unopened" to 0xAAAAAA)
            add("" to 0)
            if (list.keys.isEmpty()) {
                add((if (unopened.isEmpty()) "All chests claimed!" else "You have every key you need!") to 0x55FF55)
            } else {
                add("Keys to buy:" to 0xFFFFFF)
                for ((t, n) in list.keys) add("  ${n}x ${t.keyName}" to t.color)
                add("Materials (${f?.label ?: "faction unknown"}):" to 0xFFFFFF)
                if (list.material > 0) add("  ${list.material}x ${f?.keyMaterial ?: "Ench. Red Sand / Mycelium"}" to 0x55FFFF)
                if (list.stars > 0) add("  ${list.stars}x Nether Star" to 0x55FFFF)
                add("  %,d coins".format(list.coins) to 0xFFD700)
                add("Inventory counted; before rep discount" to 0x777777)
            }
        }

        val font = client.font
        val w = lines.maxOf { font.width(it.first) } + PAD * 2
        val h = lines.size * LINE_H + PAD * 2 - 2
        val x = (left - w - 4).coerceAtLeast(2)
        g.fill(x, top, x + w, top + h, 0xE0101018.toInt())
        g.outline(x, top, w, h, 0xFFFFAA00.toInt())
        lines.forEachIndexed { i, (text, color) -> g.text(font, text, x + PAD, top + PAD + i * LINE_H, 0xFF000000.toInt() or color) }
    }
}
