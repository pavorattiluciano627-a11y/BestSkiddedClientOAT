package dev.echo.client.features.impl.render;

import dev.echo.client.features.Category;
import dev.echo.client.features.FeatureInfo;
import dev.echo.client.features.FeatureManager;
import dev.echo.client.features.hud.EchoHudElement;
import dev.echo.client.hud.HudElementLayout;
import dev.echo.client.hud.HudElementType;
import dev.echo.client.hud.HudRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import dev.echo.client.bootstrap.CatalogText;

/** Offhand cell using the hotbar's render-eligibility policy. */
public class OffhandFeature extends EchoHudElement {
    public OffhandFeature() {
        super(new FeatureInfo(CatalogText.literal("Offhand"),
                CatalogText.literal("Live offhand item beside the hotbar"), Category.RENDER), HudElementType.OFFHAND);
    }

    @Override
    protected boolean shouldRender() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && !player.getOffhandItem().isEmpty()
                && FeatureManager.find(HotbarFeature.class)
                        .map(feature -> ((HotbarFeature) feature).canRenderHotbar()).orElse(false);
    }

    @Override
    protected void renderContents(HudElementLayout layout) {
        HudRenderer hud = HudRenderer.INSTANCE;
        float x = HudElementType.OFFHAND.defaultX();
        float y = HudElementType.OFFHAND.defaultY();
        hud.drawPanel(layout, x, y, HudElementType.OFFHAND.defaultWidth(),
                HudElementType.OFFHAND.defaultHeight(), 12.0f, 1.0f, 28.0f);
        ItemStack stack = hud.getOffhandStack();
        if (stack == null || stack.isEmpty()) {
            return;
        }
        layout.drawItem(stack, x + 14.0f, y + 13.5f, 16.0f, 10);
        if (stack.getCount() > 1) {
            layout.drawItemCount("x" + stack.getCount(), x + 25.0f, y + 25.5f, 9.5f, -419430401);
        }
    }
}
