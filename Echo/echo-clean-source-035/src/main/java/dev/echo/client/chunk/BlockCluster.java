package dev.echo.client.chunk;

/**
 * Analysis candidate for ChannelTracker: centroid in section-local block units
 * and unsigned state ID. Accessor dispatch is unproved; record naming is inferred
 * from ResponseProcessor.stopAccount's sums, divisors and constructor arguments.
 */
public record BlockCluster(double x, double y, double z, int stateId) {}
