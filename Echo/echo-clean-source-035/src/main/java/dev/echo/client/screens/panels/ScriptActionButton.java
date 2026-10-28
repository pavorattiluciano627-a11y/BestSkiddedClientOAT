package dev.echo.client.screens.panels;

/** A script action and its last rendered hit rectangle. */
final class ScriptActionButton {
    final String scriptName;
    final ScriptAction action;
    final float x;
    final float y;
    final float width;
    final float height;

    ScriptActionButton(String scriptName, ScriptAction action,
                       float x, float y, float width, float height) {
        this.scriptName = scriptName;
        this.action = action;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
}
