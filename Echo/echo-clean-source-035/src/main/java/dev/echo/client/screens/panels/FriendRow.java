package dev.echo.client.screens.panels;

import net.minecraft.client.multiplayer.PlayerInfo;

/** Friend-row candidate; its boolean constructor write still requires linkage proof. */
final class FriendRow {
    final String playerName;
    final PlayerInfo playerEntry;
    boolean friend;

    FriendRow(String playerName, PlayerInfo playerEntry, boolean friend) {
        this.playerName = playerName;
        this.playerEntry = playerEntry;
        this.friend = friend;
    }
}
