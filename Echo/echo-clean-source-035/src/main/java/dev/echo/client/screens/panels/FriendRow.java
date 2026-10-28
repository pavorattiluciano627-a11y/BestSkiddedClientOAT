package dev.echo.client.screens.panels;

import net.minecraft.client.network.PlayerListEntry;

/** Friend-row candidate; its boolean constructor write still requires linkage proof. */
final class FriendRow {
    final String playerName;
    final PlayerListEntry playerEntry;
    boolean friend;

    FriendRow(String playerName, PlayerListEntry playerEntry, boolean friend) {
        this.playerName = playerName;
        this.playerEntry = playerEntry;
        this.friend = friend;
    }
}
