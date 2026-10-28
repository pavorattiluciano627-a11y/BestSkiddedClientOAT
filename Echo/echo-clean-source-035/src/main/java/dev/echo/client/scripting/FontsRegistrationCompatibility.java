package dev.echo.client.scripting;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;

/** Preserves the shipped fonts API's rejected annotation-registration step. */
public final class FontsRegistrationCompatibility {
    private FontsRegistrationCompatibility() {
    }

    /**
     * Reports the original unsupported-parameter failure and continues initialization.
     *
     * <p>All three original fonts exports were rejected by Rhino before the host
     * constructor or instance was published. The original shared-scope and standard
     * prototype objects were not modified by that failed definition. Consequently
     * this compatibility step publishes no object and installs no font getters.</p>
     *
     * <p>The diagnostic identifier {@code addSchema} is the first rejected export
     * in the pinned Java 21 original-class metadata inspection and exact-Rhino
     * fixture. Reflection order is not asserted to be a cross-JVM invariant. The
     * public Rhino calls below are the exact expansion of reportRuntimeErrorById,
     * preserving localization, source position and the active ErrorReporter.</p>
     */
    public static void reportFailure() {
        try {
            throw Context.reportRuntimeError(
                    ScriptRuntime.getMessageById("msg.bad.parms", "long", "addSchema"));
        } catch (Exception failure) {
            failure.printStackTrace();
        }
    }
}
