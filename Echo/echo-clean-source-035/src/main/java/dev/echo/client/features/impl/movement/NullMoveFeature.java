package dev.echo.client.features.impl.movement;

import dev.echo.client.events.Subscribe;
import dev.echo.client.events.input.PlayerInputEvent;
import dev.echo.client.features.Category;
import dev.echo.client.features.Feature;
import dev.echo.client.features.FeatureInfo;
import net.minecraft.world.entity.player.Input;

/** Candidate under the callsite and guard obligations in needs.json. */
public class NullMoveFeature extends Feature {
    private boolean previousLeft;
    private boolean previousRight;
    private boolean previousForward;
    private boolean previousBackward;
    private boolean preferRight;
    private boolean preferForward;

    public NullMoveFeature() {
        super(new FeatureInfo(dev.echo.client.bootstrap.CatalogText.literal("Null Move"), dev.echo.client.bootstrap.CatalogText.literal("Instant direction changes"), dev.echo.client.features.Category.MOVEMENT, false));
    }

    @Override
    public void postInitialize() {
        previousLeft = false;
        previousRight = false;
        previousForward = false;
        previousBackward = false;
        preferRight = false;
        preferForward = false;
        super.postInitialize();
    }

    @Subscribe
    public void onMovementInput(PlayerInputEvent event) {
        if (isOutsideWorld() || client.gui.screen() != null) {
            return;
        }

        boolean pressedLeft = event.left && !previousLeft;
        boolean pressedRight = event.right && !previousRight;
        if (pressedLeft && !pressedRight) {
            preferRight = false;
        } else if (pressedRight && !pressedLeft) {
            preferRight = true;
        }
        if (event.left && event.right) {
            if (preferRight) {
                event.left = false;
            } else {
                event.right = false;
            }
        }

        boolean pressedForward = event.forward && !previousForward;
        boolean pressedBackward = event.backward && !previousBackward;
        if (pressedForward && !pressedBackward) {
            preferForward = true;
        } else if (pressedBackward && !pressedForward) {
            preferForward = false;
        }
        if (event.forward && event.backward) {
            if (preferForward) {
                event.backward = false;
            } else {
                event.forward = false;
            }
        }

        // History is the physical input, not the flags modified above.
        Input physicalInput = event.originalInput;
        previousLeft = physicalInput.left();
        previousRight = physicalInput.right();
        previousForward = physicalInput.forward();
        previousBackward = physicalInput.backward();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        previousLeft = false;
        previousRight = false;
        previousForward = false;
        previousBackward = false;
        // NB-081 resets four history flags; the two tie preferences survive.
    }

}
