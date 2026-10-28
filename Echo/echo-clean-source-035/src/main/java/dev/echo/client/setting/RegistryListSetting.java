package dev.echo.client.setting;

import dev.echo.client.setting.list.RegistryEntryGroup;
import dev.echo.client.setting.list.SelectionState;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/** Registry-backed selection; the supplied registry is resolved afresh for each operation. */
public class RegistryListSetting<T> extends Setting {
    private final Supplier<Registry<T>> registry;
    private final Set<Identifier> selectedIds = new HashSet<>();

    public RegistryListSetting(CharSequence label, Registry<T> registry) {
        this(label, () -> registry);
    }

    public RegistryListSetting(CharSequence label, Supplier<Registry<T>> registry) {
        super(label);
        this.registry = registry;
    }

    public RegistryListSetting(CharSequence label, Registry<T> registry, Predicate<Object> visibleWhen) {
        this(label, () -> registry, visibleWhen);
    }

    public RegistryListSetting(CharSequence label, Supplier<Registry<T>> registry, Predicate<Object> visibleWhen) {
        this(label, registry);
        setVisibilityPredicate(visibleWhen);
    }

    public Registry<T> getRegistry() { return registry.get(); }

    public boolean isEntrySelected(T entry) {
        if (entry == null) return false;
        ResourceKey<T> key = getRegistry().getResourceKey(entry).orElse(null);
        if (key == null) return false;
        return selectedIds.contains(key.identifier());
    }

    public boolean isIdSelected(Identifier id) { return id != null && selectedIds.contains(id); }

    public void setEntrySelected(T entry, boolean selected) {
        if (entry == null) return;
        ResourceKey<T> key = getRegistry().getResourceKey(entry).orElse(null);
        if (key == null) return;
        setIdSelected(key.identifier(), selected);
    }

    public void setIdSelected(Identifier id, boolean selected) {
        if (id == null) return;
        boolean changed = selected ? selectedIds.add(id) : selectedIds.remove(id);
        if (changed) notifyChanged();
    }

    public Set<Identifier> getSelectedIds() { return Collections.unmodifiableSet(selectedIds); }

    public void replaceSelectedIds(Collection<Identifier> ids) {
        selectedIds.clear();
        if (ids != null) selectedIds.addAll(ids);
        notifyChanged();
    }

    public void setGroupSelected(RegistryEntryGroup<T> group, boolean selected) {
        if (group == null) return;
        Registry<T> currentRegistry = getRegistry();
        boolean changed = false;
        for (T entry : currentRegistry) {
            if (!group.matches(entry)) continue;
            ResourceKey<T> key = currentRegistry.getResourceKey(entry).orElse(null);
            if (key == null) continue;
            Identifier id = key.identifier();
            if (selected ? selectedIds.add(id) : selectedIds.remove(id)) changed = true;
        }
        if (changed) notifyChanged();
    }

    public SelectionState getGroupState(RegistryEntryGroup<T> group) {
        if (group == null) return SelectionState.NONE;
        Registry<T> currentRegistry = getRegistry();
        List<T> matching = new ArrayList<>();
        for (T entry : currentRegistry) {
            if (group.matches(entry)) matching.add(entry);
        }
        if (matching.isEmpty()) return SelectionState.NONE;
        int selected = 0;
        for (T entry : matching) {
            ResourceKey<T> key = currentRegistry.getResourceKey(entry).orElse(null);
            if (key != null && selectedIds.contains(key.identifier())) selected++;
        }
        if (selected == 0) return SelectionState.NONE;
        return selected == matching.size() ? SelectionState.ALL : SelectionState.PARTIAL;
    }

    public int getSelectedCount() { return selectedIds.size(); }
    public List<RegistryEntryGroup<T>> getGroups() { return List.of(); }
    public Predicate<T> getEntryFilter() { return entry -> true; }
    public Function<T, String> getEntryLabel() { return Object::toString; }
    public Supplier<Registry<T>> getRegistrySupplier() { return registry; }
    @Override public String getTypeId() { return "reg"; }
}
