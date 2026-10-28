package dev.echo.client.utils.audio;

/** An OpenAL buffer queued with its effective gain and pitch. */
record PlaybackRequest(int bufferId, float gain, float pitch) {
}
