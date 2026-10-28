package dev.echo.client.utils.audio;

import java.nio.ShortBuffer;

/** Interleaved decoded samples with their original channel count and sample rate. */
record PcmAudio(ShortBuffer samples, int channels, int sampleRate) {
}
