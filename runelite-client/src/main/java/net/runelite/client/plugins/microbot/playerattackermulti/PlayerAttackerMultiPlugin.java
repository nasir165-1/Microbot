package net.runelite.client.plugins.microbot.playerattackermulti;

import com.google.inject.Provides;

import javax.inject.Inject;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@PluginDescriptor(
        name = "Attack Players in multi lol",
        description = "Moves to multi inside clan wars white portal and attacks players in the targetPlayer list",
        tags = {"attack", "pvp", "auto"},
        enabledByDefault = false
)
public class PlayerAttackerMultiPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private ConfigManager configManager;
    @Inject
    private PlayerAttackerMultiConfig config;
    @Inject
    private ChatMessageManager chatMessageManager;

    private Thread movementThread = null; // Store the movement thread
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, Integer> recentlyDiedPlayers = new HashMap<>();
    private long lastAttackTime = 0;
    private static final long ATTACK_COOLDOWN_MS = 2200; // adjust as needed (arrow flight + delay)


    @Provides
    PlayerAttackerMultiConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(PlayerAttackerMultiConfig.class);
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        recentlyDiedPlayers.entrySet().removeIf(entry -> entry.getValue() <= 1);
        recentlyDiedPlayers.replaceAll((name, ticks) -> ticks - 1);
        checkPlayerStatusPeriodically();
        handlePlayerDeathOrCombat();
        attackTargets();
        equipItem890();
        moveToSafeArea();       // Move to the safe area if needed// Attack targets if any
    }

    private void moveToSafeArea() {
        if (isAttacking) return; // Skip moving if attacking

        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) return;

        if (localPlayer.getInteracting() instanceof Player) {
            return;
        }

        if (System.currentTimeMillis() - lastAttackTime < ATTACK_COOLDOWN_MS) return;

        WorldPoint safePoint = new WorldPoint(config.safeX(), config.safeY(),0);
        if (localPlayer.getWorldLocation().distanceTo(safePoint) > 3 && localPlayer.getInteracting() == null) {
            if (movementThread == null || !movementThread.isAlive()) {
                movementThread = new Thread(() -> {
                    try {
                        Rs2Walker.walkTo(config.safeX(), config.safeY(),0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                movementThread.start();
            }
        }
    }


    private boolean isAttacking = false;
    private boolean isAttackScheduled = false;

    private void attackTargets() {
        if (isAttacking || isAttackScheduled) return;

        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) return;

        Player interactingPlayer = (localPlayer.getInteracting() instanceof Player) ? (Player) localPlayer.getInteracting() : null;
        if (interactingPlayer != null) return;

        List<String> targetPlayers = getTargetPlayers(); // Ordered: ["potato", "carrot", "apple"]

        for (String targetName : targetPlayers) {
            for (Player player : Rs2Player.getPlayers()) {
                if (player == null || player.getName() == null) continue;

                String playerName = player.getName().toLowerCase();
                if (!playerName.equals(targetName)) continue;

                if (recentlyDiedPlayers.containsKey(playerName)) continue;

                if (player.getAnimation() == 836) {
                    recentlyDiedPlayers.put(playerName, 8);
                    continue;
                }

                if (player.getWorldLocation().getY() > 4799) {
                    cancelMovementThread();

                    isAttacking = true;
                    isAttackScheduled = true;
                    lastAttackTime = System.currentTimeMillis();

                    executor.schedule(() -> {
                        Rs2Player.attack(player);
                        isAttacking = false;
                        isAttackScheduled = false;
                    }, 1750, TimeUnit.MILLISECONDS);

                    return;
                }
            }
        }

        isAttacking = false;
        isAttackScheduled = false;
    }



    public void checkPlayerStatusPeriodically() {
        handlePlayerDeathOrCombat();  // Periodically check and cancel movement thread if necessary
    }

    private void cancelMovementThread() {
        // If there's an active movement thread, interrupt it
        if (movementThread != null && movementThread.isAlive()) {
            movementThread.interrupt();
            movementThread = null;  // Reset the movement thread reference
        }
    }

    public void handlePlayerDeathOrCombat() {
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) return;

        // Check if the player is dead or in a death animation
        if (localPlayer.getHealthRatio() <= 0 || localPlayer.getAnimation() == 836) {
            cancelMovementThread();  // Interrupt movement thread if the player is dead
        }

        // Check if the player is interacting with a target (combat)
        Player interactingPlayer = (localPlayer.getInteracting() instanceof Player)
                ? (Player) localPlayer.getInteracting()
                : null;

        // If we are interacting with a target, cancel the movement thread
        if (interactingPlayer != null) {
            cancelMovementThread();  // Interrupt movement thread if we're in combat
        }
    }

    private void equipItem890() {
        // Get the local player
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) return;

        // Check if item 890 exists in the inventory
        if (Rs2Inventory.contains(890)) {  // Check if item 890 exists
            // Equip item 890
            boolean success = Rs2Inventory.equip(890);
            if (success) {
                System.out.println("Equipped item 890.");
            } else {
                System.out.println("Failed to equip item 890.");
            }
        }
    }

    private List<String> getTargetPlayers() {
        String configValue = config.targetPlayers();
        return (configValue == null || configValue.isEmpty()) ? List.of() : List.of(configValue.split(","));
    }
}
