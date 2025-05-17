package net.runelite.client.plugins.microbot.clanwarsbot;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;

import javax.inject.Inject;

@PluginDescriptor(
        name = "Clan Wars Bot",
        description = "A plugin to navigate and fight in Clan Wars",
        tags = {"microbot", "clan wars", "pvp", "combat"}
)
@Slf4j
public class ClanWarsBotPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private ClanWarsBotConfig config;

    @Inject
    private ConfigManager configManager;

    @Inject
    private ClanWarsBotScript clanWarsBotScript;

    private boolean running = false;

    @Provides
    ClanWarsBotConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ClanWarsBotConfig.class);
    }

    @Override
    protected void startUp() {
        log.info("Clan Wars Bot plugin started!");
        Microbot.setClient(client);

        running = true; // Set running to true to start the bot
        clanWarsBotScript.start(); // Initialize script if needed
    }

    @Override
    protected void shutDown() {
        log.info("Clan Wars Bot plugin stopped!");
        running = false; // Stop the bot
        clanWarsBotScript.shutdown(); // Clean up resources
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!running) {
            return; // Do nothing if the script is not running
        }

        if (client.getGameState() != GameState.LOGGED_IN) {
            log.warn("Game tick detected, but player is not logged in.");
            return;
        }


        // Call the bot logic on every game tick
        clanWarsBotScript.onGameTick();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOGGED_IN) {
            log.info("Player logged in.");
        } else if (event.getGameState() == GameState.LOGIN_SCREEN) { // Handle logout scenario
            log.info("Player logged out.");
        }
    }
}