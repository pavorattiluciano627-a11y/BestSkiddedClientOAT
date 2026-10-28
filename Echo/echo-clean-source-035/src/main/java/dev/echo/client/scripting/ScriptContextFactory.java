package dev.echo.client.scripting;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;

/** Creates the ES6, optimization-level-nine contexts used by Echo scripts. */
public final class ScriptContextFactory extends ContextFactory {
    /** Creates a factory; the original constructor's unused outer argument is removed. */
    public ScriptContextFactory() {
    }

    /** {@inheritDoc} */
    @Override
    protected Context makeContext() {
        Context context = super.makeContext();
        context.setOptimizationLevel(9);
        context.setLanguageVersion(Context.VERSION_ES6);
        return context;
    }
}
