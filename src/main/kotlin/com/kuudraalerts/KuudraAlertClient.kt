package com.kuudraalerts

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.decoration.ArmorStand
import java.util.UUID

object KuudraAlertClient : ClientModInitializer {

    private const val MOD_ID = "kuudraalerts"
    private const val SCAN_INTERVAL_TICKS = 5
    private const val DURATION_MS = 3000L
    private const val FADE_MS = 500L
    private const val BANNER_Y = 42

    private class Alert(val nameMatch: String, val banner: String, val color: Int, soundId: String) {
        val sound: SoundEvent = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MOD_ID, soundId))
    }

    private val ALERTS = listOf(
        Alert("Kuudra Knocker", "§c§l[!] PIGS SPAWNED!", 0xFF5555, "pigssound"),
        Alert("Smoldering Blaze", "§6§l[!] BLAZES SPAWNED!", 0xFFAA00, "blazesound"),
    )

    // Armor stands already alerted on; cleared on every level change so it never outlives one Kuudra run.
    private val alertedEntities = HashSet<UUID>()
    private var tickCounter = 0

    private var activeAlert: Alert? = null
    private var alertStartTime = 0L

    override fun onInitializeClient() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "alert_overlay")) { graphics, _ ->
            renderHud(graphics)
        }
        ClientTickEvents.END_CLIENT_TICK.register(::scan)
        // Hypixel server hops swap the level without disconnecting, so both are needed.
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register { _, _ -> reset() }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> reset() }
        VesuviusOverlay.register()
    }

    private fun reset() {
        alertedEntities.clear()
        activeAlert = null
    }

    private fun scan(client: Minecraft) {
        val level = client.level ?: return
        if (client.player == null || ++tickCounter % SCAN_INTERVAL_TICKS != 0) return

        for (entity in level.entitiesForRendering()) {
            if (entity !is ArmorStand || entity.uuid in alertedEntities) continue
            val name = entity.customName?.string ?: continue
            val alert = ALERTS.firstOrNull { it.nameMatch in name } ?: continue

            alertedEntities += entity.uuid
            activeAlert = alert
            alertStartTime = System.currentTimeMillis()
            client.soundManager.play(SimpleSoundInstance.forUI(alert.sound, 1.0f, 1.0f))
        }
    }

    private fun renderHud(graphics: GuiGraphicsExtractor) {
        val alert = activeAlert ?: return
        val remaining = DURATION_MS - (System.currentTimeMillis() - alertStartTime)
        if (remaining <= 0) {
            activeAlert = null
            return
        }

        val alpha = if (remaining < FADE_MS) (remaining * 255 / FADE_MS).toInt().coerceAtLeast(4) else 255
        val mc = Minecraft.getInstance()
        graphics.centeredText(mc.font, alert.banner, mc.window.guiScaledWidth / 2, BANNER_Y, (alpha shl 24) or alert.color)
    }
}
