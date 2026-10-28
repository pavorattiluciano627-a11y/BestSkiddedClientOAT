package dev.echo.client.bootstrap;

import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * One source-owned feature constructor and its explicit settings-initialization body.
 * The concrete catalog supplies ordinary constructor references and Java lambdas.
 */
public record FeatureDefinition(String name, Category category,
                                Supplier<? extends Feature> constructor,
                                Consumer<Feature> initializeSettings) {
}
