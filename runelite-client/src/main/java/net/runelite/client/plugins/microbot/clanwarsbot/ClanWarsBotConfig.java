package net.runelite.client.plugins.microbot.clanwarsbot;

import lombok.Getter;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

import java.util.List;

@ConfigGroup("clanwarsbot")
public interface ClanWarsBotConfig extends Config {

    // Location Settings Section
    @ConfigSection(
            name = "Location Settings",
            description = "Settings related to movement and location",
            position = 0
    )
    String locationSettings = "locationSettings";

    @ConfigItem(
            keyName = "selectedLocation",
            name = "Location",
            description = "Select the location the bot should navigate to",
            section = locationSettings
    )
    default ClanWarsLocation selectedLocation() {
        return ClanWarsLocation.CLAN_WARS_MULTI; // Default location
    }

    @ConfigItem(
            keyName = "customLocationX",
            name = "Custom Location X",
            description = "X coordinate for a custom location",
            section = locationSettings
    )
    @Range(
            min = 0,
            max = 5000
    )
    default int customLocationX() {
        return 0;
    }

    @ConfigItem(
            keyName = "customLocationY",
            name = "Custom Location Y",
            description = "Y coordinate for a custom location",
            section = locationSettings
    )
    @Range(
            min = 0,
            max = 5000
    )
    default int customLocationY() {
        return 0;
    }

    @ConfigItem(
            keyName = "movementRange",
            name = "Movement Range",
            description = "The distance considered as 'reached' for a target location",
            section = locationSettings
    )
    @Range(
            min = 1,
            max = 10
    )
    default int movementRange() {
        return 2;
    }

    // Combat Settings Section
    @ConfigSection(
            name = "Combat Settings",
            description = "Settings related to combat and attacks",
            position = 10
    )
    String combatSettings = "combatSettings";

    @ConfigItem(
            keyName = "targetNames",
            name = "Target Names",
            description = "Comma or newline separated list of player names to attack",
            section = combatSettings
    )
    default String targetNames() {
        return "";
    }

    @ConfigItem(
            keyName = "attackRange",
            name = "Attack Range",
            description = "The distance from which the bot should attack a player",
            section = combatSettings
    )
    @Range(
            min = 1,
            max = 20
    )
    default int attackRange() {
        return 5;
    }
}