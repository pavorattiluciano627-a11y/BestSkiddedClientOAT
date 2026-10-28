package dev.echo.client.screens.panels;

/** A friend name and the last rendered rectangle of its row action. */
final class FriendActionButton {
    final String playerName;
    final float x;
    final float y;
    final float width;
    final float height;

    FriendActionButton(String playerName, float x, float y, float width, float height) {
        this.playerName = playerName;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}
