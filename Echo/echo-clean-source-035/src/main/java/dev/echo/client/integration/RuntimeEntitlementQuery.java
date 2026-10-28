package dev.echo.client.integration;

/** Lazily caches the runtime's beta entitlement response. */
public final class RuntimeEntitlementQuery {
    private static Boolean beta;

    private RuntimeEntitlementQuery() {
    }

    /**
     * Queries beta access once a response is successfully obtained.
     *
     * <p>Only the exact response {@code "true"} grants access. A failed command
     * propagates its exception and leaves an empty cache available for retry.
     * As in the original service, concurrent first calls are not serialized.
     *
     * @return the cached entitlement result
     */
    public static boolean isBeta() {
        if (beta == null) {
            beta = "true".equals(OfflineRuntimeSession.shared().call("beta", new String[0]));
        }
        return beta;
    }
}
