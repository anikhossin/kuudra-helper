package com.kuudraalerts;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.Entity;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class KuudraAlertClient implements ClientModInitializer {

    private static final String MOD_ID = "kuudraalerts";
    private static final SoundEvent PIGS_SOUND = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MOD_ID, "pigssound"));
    private static final SoundEvent BLAZES_SOUND = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MOD_ID, "blazesound"));
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(MOD_ID, "alert_overlay");

    private static final Set<UUID> alertedEntities = new HashSet<UUID>();
    private static int tickCounter = 0;

    // HUD state
    private static String currentAlertText = null;
    private static long alertStartTime = 0L;
    private static int alertColor = 0xFF5555;
    private static final long DURATION_MS = 3000L;
    private static final long FADE_MS = 500L;

    @Override
    public void onInitializeClient() {
        // Register HUD element
        HudElementRegistry.addLast(HUD_ID, (GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) -> {
            renderHud(graphics);
        });

        // Register client tick entity scanner
        ClientTickEvents.END_CLIENT_TICK.register((Minecraft client) -> {
            if (client.level == null || client.player == null) return;

            tickCounter++;
            if (tickCounter % 5 != 0) return;

            for (Entity entity : client.level.entitiesForRendering()) {
                if (!(entity instanceof ArmorStand)) continue;
                if (alertedEntities.contains(entity.getUUID())) continue;

                if (entity.getCustomName() == null) continue;
                String customName = entity.getCustomName().getString();

                if (customName.contains("Kuudra Knocker")) {
                    triggerAlert(client, entity.getUUID(), "§c§l[!] PIGS SPAWNED!", 0xFF5555, PIGS_SOUND);
                } else if (customName.contains("Smoldering Blaze")) {
                    triggerAlert(client, entity.getUUID(), "§6§l[!] BLAZES SPAWNED!", 0xFFAA00, BLAZES_SOUND);
                }
            }
        });

        // Clear state on disconnect / world unload
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            alertedEntities.clear();
            currentAlertText = null;
            alertStartTime = 0L;
        });
    }

    private static void triggerAlert(Minecraft client, UUID uuid, String bannerText, int color, SoundEvent sound) {
        alertedEntities.add(uuid);
        currentAlertText = bannerText;
        alertColor = color;
        alertStartTime = System.currentTimeMillis();

        // Play client UI sound directly to headphones
        client.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0f, 1.0f));
    }

    private static void renderHud(GuiGraphicsExtractor graphics) {
        if (currentAlertText == null) return;
        long elapsed = System.currentTimeMillis() - alertStartTime;
        if (elapsed > DURATION_MS) {
            currentAlertText = null;
            return;
        }

        int alpha = 255;
        long timeRemaining = DURATION_MS - elapsed;
        if (timeRemaining < FADE_MS) {
            alpha = (int) ((timeRemaining / (float) FADE_MS) * 255.0f);
            if (alpha < 4) alpha = 4;
        }

        int finalColor = (alpha << 24) | (alertColor & 0x00FFFFFF);
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int x = screenWidth / 2;
        int y = 42;

        graphics.centeredText(mc.font, currentAlertText, x, y, finalColor);
    }
}
