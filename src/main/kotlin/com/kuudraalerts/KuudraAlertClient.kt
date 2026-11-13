package com.kuudraalerts

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.DeltaTracker
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.decoration.ArmorStand
import java.util.UUID

object KuudraAlertClient : ClientModInitializer {

    private const val MOD_ID = "kuudraalerts"
    private val PIGS_SOUND = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MOD_ID, "pigssound"))
    private val BLAZES_SOUND = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MOD_ID, "blazesound"))
    private val HUD_ID = Identifier.fromNamespaceAndPath(MOD_ID, "alert_overlay")

    private val alertedEntities = HashSet<UUID>()
    private var tickCounter = 0

    // HUD state
    private var currentAlertText: String? = null
    private var alertStartTime = 0L
    private var alertColor = 0xFF5555
    private const val DURATION_MS = 3000L
    private const val FADE_MS = 500L

    override fun onInitializeClient() {
        // Register HUD element
        HudElementRegistry.addLast(HUD_ID) { graphics: GuiGraphicsExtractor, _: DeltaTracker ->
            renderHud(graphics)
        }

        // Register client tick entity scanner
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val level = client.level ?: return@register
            if (client.player == null) return@register

            tickCounter++
            if (tickCounter % 5 != 0) return@register

            for (entity in level.entitiesForRendering()) {
                if (entity !is ArmorStand) continue
                if (alertedEntities.contains(entity.uuid)) continue

                val customName = entity.customName?.string ?: continue

                if (customName.contains("Kuudra Knocker")) {
                    triggerAlert(client, entity.uuid, "§c§l[!] PIGS SPAWNED!", 0xFF5555, PIGS_SOUND)
                } else if (customName.contains("Smoldering Blaze")) {
                    triggerAlert(client, entity.uuid, "§6§l[!] BLAZES SPAWNED!", 0xFFAA00, BLAZES_SOUND)
                }
            }
        }

        // Clear state on disconnect / world unload
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            alertedEntities.clear()
            currentAlertText = null
            alertStartTime = 0L
        }
    }

    private fun triggerAlert(client: Minecraft, uuid: UUID, bannerText: String, color: Int, sound: SoundEvent) {
        alertedEntities.add(uuid)
        currentAlertText = bannerText
        alertColor = color
        alertStartTime = System.currentTimeMillis()

        // Play client UI sound directly to headphones
        client.soundManager.play(SimpleSoundInstance.forUI(sound, 1.0f, 1.0f))
    }

    private fun renderHud(graphics: GuiGraphicsExtractor) {
        val text = currentAlertText ?: return
        val elapsed = System.currentTimeMillis() - alertStartTime
        if (elapsed > DURATION_MS) {
            currentAlertText = null
            return
        }

        var alpha = 255
        val timeRemaining = DURATION_MS - elapsed
        if (timeRemaining < FADE_MS) {
            alpha = ((timeRemaining / FADE_MS.toFloat()) * 255.0f).toInt().coerceAtLeast(4)
        }

        val finalColor = (alpha shl 24) or (alertColor and 0x00FFFFFF)
        val mc = Minecraft.getInstance()
        val screenWidth = mc.window.guiScaledWidth
        val x = screenWidth / 2
        val y = 42

        graphics.centeredText(mc.font, text, x, y, finalColor)
    }
}
