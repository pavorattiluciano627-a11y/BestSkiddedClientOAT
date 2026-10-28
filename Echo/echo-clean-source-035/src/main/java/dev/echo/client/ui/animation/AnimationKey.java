package dev.echo.client.ui.animation;

/**
 * Identifies an animation by component identity and property name.
 *
 * @param componentId the owning UI component's stable identity
 * @param propertyName the property being animated
 */
record AnimationKey(long componentId, String propertyName) {
}
