package net.runelite.client.plugins.microbot.clanwarsbot;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class SharedSettingsManager {
    private static final String SETTINGS_FILE = System.getProperty("user.home") + "/microbot_settings.txt";
    private static final long CHECK_INTERVAL_MS = 2000; // Check for changes every 2 seconds

    private Map<String, String> settings = new HashMap<>();
    private long lastModified = 0;
    private ScheduledExecutorService scheduler;
    private Runnable onSettingsChangedCallback;

    // Singleton instance
    private static SharedSettingsManager instance;

    public static SharedSettingsManager getInstance() {
        if (instance == null) {
            instance = new SharedSettingsManager();
        }
        return instance;
    }

    private SharedSettingsManager() {
        // Create default settings file if it doesn't exist
        File file = new File(SETTINGS_FILE);
        if (!file.exists()) {
            createDefaultSettingsFile();
        }

        // Initial load of settings
        loadSettings();
    }

    private void createDefaultSettingsFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(SETTINGS_FILE))) {
            writer.println("LOCATION=CLAN_WARS_MULTI");
            writer.println("CUSTOM_X=3330");
            writer.println("CUSTOM_Y=4800");
            writer.println("MOVEMENT_RANGE=10");
            writer.println("ATTACK_RANGE=1");
            writer.println("TARGET_LIST=Player1,Player2,Player3");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startMonitoring(Runnable onSettingsChanged) {
        this.onSettingsChangedCallback = onSettingsChanged;

        if (scheduler != null) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            File file = new File(SETTINGS_FILE);
            if (file.exists() && file.lastModified() > lastModified) {
                loadSettings();
                if (onSettingsChangedCallback != null) {
                    onSettingsChangedCallback.run();
                }
            }
        }, CHECK_INTERVAL_MS, CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public void stopMonitoring() {
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
    }

    private void loadSettings() {
        File file = new File(SETTINGS_FILE);
        if (!file.exists()) {
            return;
        }

        try {
            lastModified = file.lastModified();
            List<String> lines = Files.readAllLines(file.toPath());

            Map<String, String> newSettings = new HashMap<>();
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // Skip comments and empty lines
                }

                int equalsPos = line.indexOf('=');
                if (equalsPos > 0) {
                    String key = line.substring(0, equalsPos).trim();
                    String value = line.substring(equalsPos + 1).trim();
                    newSettings.put(key, value);
                }
            }

            settings = newSettings;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getSetting(String key) {
        return settings.getOrDefault(key, "");
    }

    public String getSetting(String key, String defaultValue) {
        return settings.getOrDefault(key, defaultValue);
    }

    public int getIntSetting(String key, int defaultValue) {
        String value = settings.get(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public List<String> getTargetList() {
        String targetListStr = getSetting("TARGET_LIST", "");
        if (targetListStr.isEmpty()) {
            return new ArrayList<>();
        }

        return Arrays.stream(targetListStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public ClanWarsBotLocation getLocation() {
        String locationStr = getSetting("LOCATION", "CLAN_WARS_MULTI");
        try {
            return ClanWarsBotLocation.valueOf(locationStr);
        } catch (IllegalArgumentException e) {
            return ClanWarsBotLocation.CLAN_WARS_MULTI; // Default if invalid
        }
    }

    public int getCustomX() {
        return getIntSetting("CUSTOM_X", 3330);
    }

    public int getCustomY() {
        return getIntSetting("CUSTOM_Y", 4800);
    }

    public int getMovementRange() {
        return getIntSetting("MOVEMENT_RANGE", 10);
    }

    public int getAttackRange() {
        return getIntSetting("ATTACK_RANGE", 1);
    }
}