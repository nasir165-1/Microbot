package net.runelite.client.plugins.microbot.playerattackermulti;

import net.runelite.client.config.*;

@ConfigGroup("playerattackermulti")
public interface PlayerAttackerMultiConfig extends Config {


    @ConfigSection(
            name = "Targets",
            description = "List of target players",
            position = 2
    )
    String targetsSection = "targetsSection";

    @ConfigItem(
            keyName = "targetPlayers",
            name = "Target Players",
            description = "Comma-separated list of players that should be attacked.",
            section = targetsSection
    )
    default String targetPlayers() {
        return "";
    }
    @ConfigItem(
            keyName = "safeX",
            name = "Safe spot X",
            description = "X coordinate of the safe location",
            position = 0
    )
    default int safeX() {
        return 3330;
    }

    @ConfigItem(
            keyName = "safeY",
            name = "Safe spot Y",
            description = "Y coordinate of the safe location",
            position = 1
    )
    default int safeY() {
        return 4797;
    }
}
