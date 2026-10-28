package dev.echo.client.screens.dashboard;

import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;

/** Sidebar row; a null feature is the original category-heading row. */
public record FeatureRow(Category category, Feature feature, float y, float height, float visibility) {
}
