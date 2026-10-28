package dev.echo.client.events.render;

import dev.echo.client.events.Event;
import dev.echo.client.events.EventBus;
import dev.echo.client.events.input.KeybindingEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;

/**
 * Mutable text selection and style-preserving text transformations.
 * This functional analysis candidate preserves the original shared-sentinel branches;
 * linkage/cache and production state-removal obligations are recorded separately.
 */
public class TextTransformEvent extends Event {
    /** Text being transformed, replaceable by synchronous subscribers. */
    public String text;

    private TextTransformEvent(String text) {
        this.text = text;
    }

    private static boolean shouldSkipTransformation() {
        String[] sentinel = KeybindingEvent.getInputSentinel();
        boolean subscribed = EventBus.hasSubscribers(TextTransformEvent.class);
        return sentinel != null ? subscribed : !subscribed;
    }

    /**
     * Applies subscriber changes to nonempty text.
     * @param text original string
     * @return the subscriber-selected string
     */
    public static String transform(String text) {
        String[] sentinel = KeybindingEvent.getInputSentinel();
        if (sentinel != null || text.isEmpty() || shouldSkipTransformation()) {
            return text;
        }
        TextTransformEvent event = new TextTransformEvent(text);
        event.post();
        return event.text;
    }

    /**
     * Transforms styled strings and retains the original when the change flag is unset.
     * @param text original styled text
     * @return collected segments when marked changed, otherwise the original object
     */
    public static StringVisitable transform(StringVisitable text) {
        String[] sentinel = KeybindingEvent.getInputSentinel();
        if (shouldSkipTransformation()) {
            return text;
        }
        List<StringVisitable> fragments = new ArrayList<>();
        boolean[] changed = {false};
        if (sentinel != null) {
            return text;
        }
        text.visit((style, string) -> {
            String[] segmentSentinel = KeybindingEvent.getInputSentinel();
            String transformed = transform(string);
            boolean equal = transformed.equals(string);
            changed[0] |= segmentSentinel != null ? equal : !equal;
            fragments.add(StringVisitable.styled(transformed, style));
            return Optional.empty();
        }, Style.EMPTY);
        return changed[0] ? StringVisitable.concat(fragments) : text;
    }

    /**
     * Transforms contiguous equal-style code-point runs independently.
     * Changed runs are emitted using forward ordered text, as in the original.
     * @param text original ordered text
     * @return transformed runs, or the original object when contents are unchanged
     */
    public static OrderedText transform(OrderedText text) {
        String[] sentinel = KeybindingEvent.getInputSentinel();
        if (shouldSkipTransformation()) {
            return text;
        }
        // The original non-null sentinel branch can place a StringBuilder here.
        // Retain that path and its later cast failures instead of assuming null.
        List<Object> styles = new ArrayList<>();
        List<StringBuilder> runs = new ArrayList<>();
        text.accept((index, style, codePoint) -> {
            String[] characterSentinel = KeybindingEvent.getInputSentinel();
            if (characterSentinel != null) {
                styles.add(new StringBuilder());
            } else if (styles.isEmpty() || !style.equals(styles.get(styles.size() - 1))) {
                styles.add(style);
                runs.add(new StringBuilder());
            }
            runs.get(runs.size() - 1).appendCodePoint(codePoint);
            return true;
        });

        boolean changed = false;
        List<OrderedText> transformedRuns = new ArrayList<>(runs.size());
        for (int index = 0; index < runs.size(); index++) {
            if (sentinel != null) {
                return text;
            }
            StringBuilder run = runs.get(index);
            String transformed = transform(run.toString());
            changed |= !transformed.contentEquals(run);
            transformedRuns.add(OrderedText.styledForwardsVisitedString(transformed, (Style) styles.get(index)));
        }
        return changed ? OrderedText.concat(transformedRuns) : text;
    }
}
