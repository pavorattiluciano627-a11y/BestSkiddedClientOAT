package dev.echo.client.screens.dashboard;

import java.util.ArrayList;
import java.util.List;

/** Original dashboard rules for local/public configurations and their copied profiles. */
public final class ConfigCardRules {
    /** Original controller publishing-entitlement query. */
    public interface PublishingAccess {
        boolean canPublish();
    }

    private final List<ConfigCard> cards;
    private final PublishingAccess publishing;

    /** Retains the controller's live ordered card list instead of taking a snapshot. */
    public ConfigCardRules(List<ConfigCard> cards, PublishingAccess publishing) {
        this.cards = cards;
        this.publishing = publishing;
    }

    /** Finds the first exact ID match, rejecting null and blank search IDs. */
    public ConfigCard find(String id) {
        if (id == null || id.isBlank()) return null;
        for (ConfigCard card : cards) {
            if (card.id.equals(id)) return card;
        }
        return null;
    }

    /** Returns the first local copy whose source ID identifies the given public card. */
    public ConfigCard firstCopy(ConfigCard source) {
        for (ConfigCard card : cards) {
            if (!card.publicVisibility && card.sourceId.equals(source.id)) return card;
        }
        return null;
    }

    /** Counts local copies without deduplicating their source IDs. */
    public int copyCount(ConfigCard source) {
        int count = 0;
        for (ConfigCard card : cards) {
            if (!card.publicVisibility && card.sourceId.equals(source.id)) count++;
        }
        return count;
    }

    /** Selects the original primary button identity for a card. */
    public ConfigPrimaryAction primaryAction(ConfigCard card) {
        if (!card.publicVisibility) return ConfigPrimaryAction.LOAD;
        if (card.owned) return ConfigPrimaryAction.LOAD_OWN;
        int copies = copyCount(card);
        if (copies > 1) return ConfigPrimaryAction.SHOW_COPIES;
        return copies == 1 ? ConfigPrimaryAction.LOAD_COPY : ConfigPrimaryAction.ADD;
    }

    /** Determines whether the card represents an available local load target. */
    public boolean hasLocalLoadTarget(ConfigCard card) {
        if (!card.publicVisibility) return true;
        if (card.owned) return false;
        return firstCopy(card) != null;
    }

    /** Preserves case-insensitive local-name collision checking and public-card exclusion. */
    public boolean localNameExists(String name) {
        for (ConfigCard card : cards) {
            if (!card.publicVisibility && card.name.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    /** Updates selected state in iteration order after the controller changes active ID. */
    public void select(String activeId) {
        for (ConfigCard card : cards) {
            card.selected = card.id.equals(activeId);
        }
    }

    /**
     * Returns the original context-menu action order. The publishing permission is
     * the actual result of the controller's entitlement query, not an approval stub.
     */
    public List<ConfigMenuAction> menuActions(ConfigCard card) {
        List<ConfigMenuAction> actions = new ArrayList<>();
        if (card.publicVisibility) {
            actions.add(ConfigMenuAction.ADD_COPY);
            return actions;
        }
        actions.add(ConfigMenuAction.EDIT);
        if (publishing.canPublish()) {
            actions.add(card.isPublished() ? ConfigMenuAction.UPDATE_PUBLIC : ConfigMenuAction.PUBLISH);
        }
        if (card.selected && card.updateAvailable) actions.add(ConfigMenuAction.GET_UPDATE);
        if (card.isPublished()) actions.add(ConfigMenuAction.UNPUBLISH);
        actions.add(ConfigMenuAction.DELETE);
        return actions;
    }
}
