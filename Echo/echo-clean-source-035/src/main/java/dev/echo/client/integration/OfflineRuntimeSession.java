package dev.echo.client.integration;

import java.util.Objects;

/**
 * The supplied client's offline command protocol and local session lifecycle.
 * Profile commands preserve its operation-specific offline failure responses.
 */
public final class OfflineRuntimeSession {
    private static final OfflineRuntimeSession SHARED = new OfflineRuntimeSession();

    private static final String PROFILE_LIST_FAILURE =
            "{\"success\":false,\"error\":\"Cloud service unavailable offline\",\"activeConfigId\":\"\",\"configs\":[]}";
    private static final String PROFILE_FAILURE =
            "{\"success\":false,\"error\":\"Cloud service unavailable offline\",\"activeConfigId\":\"\",\"config\":null,\"data\":\"\"}";
    private static final String PROFILE_DELETE_FAILURE =
            "{\"success\":false,\"error\":\"Cloud service unavailable offline\",\"deletedConfigId\":\"\",\"activeConfigId\":\"\"}";

    private volatile boolean initialized;

    /**
     * Returns the client-wide runtime session used by direct service callers.
     * @return this class loader's shared session
     */
    public static OfflineRuntimeSession shared() {
        return SHARED;
    }

    /** Records successful establishment of the offline session during bootstrap. */
    public void markInitialized() {
        initialized = true;
    }

    /**
     * Executes a case-sensitive command from the existing runtime protocol.
     * All arguments are checked before dispatch, even when the command ignores them.
     *
     * @param operation exact protocol operation name
     * @param arguments command arguments, or {@code null} for an empty argument list
     * @return the protocol response, or empty text for an unknown operation
     * @throws NullPointerException if the operation or any supplied argument is null
     */
    public String call(String operation, String[] arguments) {
        Objects.requireNonNull(operation, "operation");
        if (arguments != null) {
            for (int index = 0; index < arguments.length; index++) {
                Objects.requireNonNull(arguments[index], "arguments[" + index + "]");
            }
        }

        // These are literal command IDs of the native API, not member-lookup keys.
        return switch (operation) {
            case "uid" -> initialized ? "0" : "";
            case "is_available" -> "true";
            case "list_profiles_json" -> PROFILE_LIST_FAILURE;
            case "get_profile_json", "save_profile_json", "select_profile_json" -> PROFILE_FAILURE;
            case "delete_profile_json" -> PROFILE_DELETE_FAILURE;
            default -> "";
        };
    }
}
