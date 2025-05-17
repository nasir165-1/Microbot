package net.runelite.client.plugins.microbot.clanwarsbot;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ClanWarsBotScript {
    @Inject
    private ClanWarsBotConfig config;

    private boolean initialized = false;
    private WorldPoint targetLocation;
    private final List<WorldPoint> locationCoords = new ArrayList<>();
    private int currentLocationIndex = 0;

    private long lastMovementTime = 0;
    private static final long MOVEMENT_COOLDOWN = 1000;
    private final AtomicBoolean isMovementThreadActive = new AtomicBoolean(false);

    private int[] portalXs = {3127, 3128, 3129};
    private int portalY = 3626;
    private int plane = 0;
    private int currentTileIdx = 0;

    public void start() {
        initialized = true;
        initializeLocations();

        if (locationCoords.isEmpty()) {
            log.error("No valid locations initialized! Please check the configuration.");
            initialized = false;
        } else {
            updateTargetLocation();
            log.info("Bot initialized. Current target location: {}", targetLocation);
        }
    }

    public void shutdown() {
        initialized = false;
        cancelMovementThread();
        log.info("Bot shut down.");
    }

    public void onConfigChanged() {
        if (!initialized) {
            log.warn("Bot is not initialized. Ignoring configuration change.");
            return;
        }

        log.info("Configuration changed. Cancelling active threads and reinitializing locations...");
        cancelMovementThread();
        initializeLocations();
        updateTargetLocation();
    }

    public void onGameTick() {
        if (!initialized) {
            log.info("Not initialized. Skipping game tick.");
            return;
        }

        WorldPoint currentLocation = Rs2Player.getWorldLocation();
        if (currentLocation != null && currentLocation.getY() < 4000) {
            // Still check for portal interaction if below 3700
            interactPortal();
            handleCombat();
            return;
        }

        // Portal check even if above 4000
        interactPortal();
        log.warn("portal scan is working");



        WorldPoint destination = getDestinationForSelectedLocation();
        List<String> targetNames = getTargetNames();

        if (destination != null && currentLocation != null) {
            int distance = currentLocation.distanceTo(destination);
            log.info("Distance to destination {}: {}", destination, distance);

            if (distance > config.movementRange()) {
                if (isInteractingWithTarget(targetNames)) {
                    log.info("Currently interacting with a target player; will not move to safe area.");
                } else {
                    log.info("Outside movement range ({} > {}). Moving to destination.", distance, config.movementRange());
                    moveToSafeArea(destination);
                }
            } else {
                log.info("Within movement range. No movement needed.");
            }
        }

        handleCombat();
    }



    public boolean interactPortal() {
        // Check if player is below y 4000

        // Interact with the object using the "Enter" option
        return Rs2GameObject.interact(26645,"Enter");
    }



    /// //////////////////////////
    private WorldPoint getDestinationForSelectedLocation() {
        switch (config.selectedLocation()) {
            case CLAN_WARS_MULTI:
                return new WorldPoint(3330, 4815, 0);
            case CLAN_WARS_HILLS:
                return new WorldPoint(3330, 4799, 0);
            case CLAN_WARS_SINGLES:
                return new WorldPoint(3330, 4765, 0);
            case CLAN_WARS_POND:
                return new WorldPoint(3351, 4813, 0);
            case CLAN_WARS_LAVA:
                return new WorldPoint(3312, 4813, 0);
            case CUSTOM_LOCATION:
                return new WorldPoint(config.customLocationX(), config.customLocationY(), 0);
            default:
                log.warn("Invalid location selected: {}", config.selectedLocation());
                return null;
        }
    }


    private void moveToSafeArea(WorldPoint safeLocation) {
        targetLocation = safeLocation;
        moveToTargetLocation();
    }

    private void moveToTargetLocation() {
        if (targetLocation == null) {
            log.warn("Target location is null. Cannot move.");
            return;
        }

        if (isMovementThreadActive.get()) {
            log.info("Movement thread is already active.");
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMovementTime < MOVEMENT_COOLDOWN) {
            return;
        }

        log.info("Attempting to walk to {}", targetLocation);

        isMovementThreadActive.set(true);
        new Thread(() -> {
            try {
                boolean success = Rs2Walker.walkTo(targetLocation, config.movementRange());
                log.info("Rs2Walker.walkTo returned {}", success);
            } catch (Throwable t) {
                log.error("Exception in walker thread", t);
            } finally {
                isMovementThreadActive.set(false);
            }
        }).start();
        lastMovementTime = currentTime;
    }

    private void cancelMovementThread() {
        if (isMovementThreadActive.get()) {
            log.info("Cancelling active movement thread...");
            isMovementThreadActive.set(false);
        }
    }

    private void initializeLocations() {
        locationCoords.clear();
        log.info("Initializing locations for selected location: {}", config.selectedLocation());

        switch (config.selectedLocation()) {
            case CLAN_WARS_MULTI:
                locationCoords.add(new WorldPoint(3330, 4815, 0));
                break;
            case CLAN_WARS_SINGLES:
                locationCoords.add(new WorldPoint(3330, 4765, 0));
                break;
            case CLAN_WARS_POND:
                locationCoords.add(new WorldPoint(3341, 4813, 0));
                break;
            case CLAN_WARS_LAVA:
                locationCoords.add(new WorldPoint(3312, 4813, 0));
                break;
            case CLAN_WARS_HILLS:
                locationCoords.add(new WorldPoint(3330, 4799, 0));
                break;
            case CUSTOM_LOCATION:
                locationCoords.add(new WorldPoint(
                        config.customLocationX(),
                        config.customLocationY(),
                        0
                ));
                break;
            default:
                break;
        }

        if (!locationCoords.isEmpty()) {
            updateTargetLocation();
        } else {
        }
    }

    private void updateTargetLocation() {
        if (locationCoords.isEmpty()) {
            return;
        }
        targetLocation = locationCoords.get(currentLocationIndex);
    }

    // --- Combat Logic ---
    private void handleCombat() {
        // Prevent attacking while moving
        if (isMovementThreadActive.get()) {
            return;
        }

        Player localPlayer = Rs2Player.getLocalPlayer();
        if (localPlayer != null) {
            int localHealth = localPlayer.getHealthRatio();
            int localAnimation = localPlayer.getAnimation();
            if ((localHealth >= 0 && localHealth < 1) || localAnimation == 836) {
                log.info("Skipping combat: we're dead or playing death animation (836).");
                return;
            }
        }

        List<String> targetNames = getTargetNames();

        // Prevent attacking if already interacting with a target
        if (isInteractingWithTarget(targetNames)) {
            return;
        }

        List<Player> players = Rs2Player.getPlayers();
        if (players == null) {
            return;
        }

        WorldPoint myLoc = Rs2Player.getWorldLocation();

        for (Player player : players) {
            if (player == null || player.getName() == null) continue;

            String playerNameLower = player.getName().toLowerCase();
            boolean isTarget = targetNames.stream()
                    .anyMatch(name -> playerNameLower.equals(name.toLowerCase()));

            if (!isTarget) continue;

            // --- Skip dead/dying players ---
            int healthRatio = player.getHealthRatio();
            int animation = player.getAnimation();
            if (healthRatio >= 0 && healthRatio < 1) {
                continue;
            }
            if (animation == 836) {

                continue;
            }

            WorldPoint targetLoc = player.getWorldLocation();
            if (targetLoc == null || myLoc == null) continue;

            ClanWarsLocation selected = config.selectedLocation();

            // MULTI, POND, LAVA, HILLS: Only attack if target Y > 4799
            if (selected == ClanWarsLocation.CLAN_WARS_MULTI
                    || selected == ClanWarsLocation.CLAN_WARS_POND
                    || selected == ClanWarsLocation.CLAN_WARS_LAVA
                    || selected == ClanWarsLocation.CLAN_WARS_HILLS) {
                if (targetLoc.getY() <= 4799) {
                    continue;
                }
            }

            // SINGLES: Both must be above Y 4759, neither in combat
            if (selected == ClanWarsLocation.CLAN_WARS_SINGLES) {
                if (myLoc.getY() <= 4759 || targetLoc.getY() <= 4759) {

                    continue;
                }
                if (localPlayer != null && localPlayer.getInteracting() != null) {
                    continue;
                }
                if (player.getInteracting() != null) {
                    continue;
                }
            }

            // CUSTOM: If we're within 4760-4799, only attack targets in that range, neither in combat
            if (selected == ClanWarsLocation.CUSTOM_LOCATION) {
                if (myLoc.getY() > 4759 && myLoc.getY() < 4800) {
                    if (targetLoc.getY() <= 4759 || targetLoc.getY() >= 4800) {
                        continue;
                    }
                    if (localPlayer != null && localPlayer.getInteracting() != null) {
                        continue;
                    }
                    if (player.getInteracting() != null) {
                        continue;
                    }
                }
            }

            int distance = myLoc.distanceTo(targetLoc);

            if (distance <= config.attackRange()) {
                log.info("Attacking '{}'", player.getName());
                Rs2Player.attack(player);
            } else {
            }
            return; // Only attack one target per tick
        }
    }

    private Player findTargetPlayer(List<String> targetNames) {
        List<Player> players = Rs2Player.getPlayers();
        if (players == null) return null;
        WorldPoint myLoc = Rs2Player.getWorldLocation();

        // Convert all target names to lower case for case-insensitive comparison
        List<String> lowerTargetNames = targetNames.stream()
                .filter(name -> name != null)
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        for (Player player : players) {
            if (player == null || player.getName() == null)
                continue;
            String playerNameLower = player.getName().toLowerCase();
            if (lowerTargetNames.contains(playerNameLower)) {
                int distance = myLoc.distanceTo(player.getWorldLocation());
                if (distance <= config.attackRange()) {
                    log.info("Target found: {} at distance {}", player.getName(), distance);
                    return player;
                }
            }
        }
        return null;
    }

    private boolean isInteractingWithTarget(List<String> targetNames) {
        Player localPlayer = Rs2Player.getLocalPlayer();
        if (localPlayer == null) return false;

        Player interacting = (Player) localPlayer.getInteracting();
        if (interacting == null || interacting.getName() == null) return false;

        String interactingName = interacting.getName().toLowerCase();
        return targetNames.stream().anyMatch(name -> interactingName.equals(name.toLowerCase()));
    }

    private List<String> getTargetNames() {
        String raw = config.targetNames();
        if (raw == null || raw.trim().isEmpty()) return List.of();
        return Arrays.stream(raw.split("[,\\n]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }


    private void attackPlayer(Player player) {
        if (player == null) {
            log.warn("Cannot attack. Player is null.");
            return;
        }

        try {
            log.info("Attacking player: {}", player.getName());
            Rs2Player.attack(player);
        } catch (Exception e) {
            log.error("Error while attacking player: {}", player.getName(), e);
        }
    }
}