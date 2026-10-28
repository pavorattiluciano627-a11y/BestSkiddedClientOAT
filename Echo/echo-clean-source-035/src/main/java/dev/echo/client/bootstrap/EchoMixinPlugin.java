package dev.echo.client.bootstrap;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/** Applies optional integrations only when their target class is available. */
public final class EchoMixinPlugin implements IMixinConfigPlugin {
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return targetClassName.startsWith("net.minecraft.")
                || targetClassName.startsWith("com.mojang.")
                || isTargetAvailable(targetClassName);
    }

    private static boolean isTargetAvailable(String className) {
        try (InputStream stream = MixinService.getService()
                .getResourceAsStream(className.replace('.', '/') + ".class")) {
            return stream != null;
        } catch (Throwable unavailable) {
            return false;
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null; // Use the refmap declared in the mixin configuration.
    }

    @Override
    public List<String> getMixins() {
        return null; // All mixins are listed explicitly in the configuration.
    }

    @Override
    public void onLoad(String mixinPackage) {
        // The original plugin has no initialization callback behavior.
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // The original plugin leaves both target sets unchanged.
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // No additional bytecode transformation precedes Mixin's application.
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // No additional bytecode transformation follows Mixin's application.
    }
}
