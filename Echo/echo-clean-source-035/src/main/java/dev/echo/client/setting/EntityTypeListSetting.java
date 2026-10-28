package dev.echo.client.setting;

import dev.echo.client.setting.list.RegistryEntryGroup;
import dev.echo.client.text.SegmentedText;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;

/** Entity-type selection with the original four preset groups. */
public class EntityTypeListSetting extends RegistryListSetting<EntityType<?>> {
    private static final List<RegistryEntryGroup<EntityType<?>>> GROUPS = List.of(
            new RegistryEntryGroup<>(SegmentedText.literal("Players"), type -> type == EntityType.PLAYER),
            new RegistryEntryGroup<>(SegmentedText.literal("Hostile"), type -> type.getSpawnGroup() == SpawnGroup.MONSTER),
            new RegistryEntryGroup<>(SegmentedText.literal("Passive"), type -> type != EntityType.PLAYER
                    && type.getSpawnGroup() != SpawnGroup.MONSTER && type.getSpawnGroup() != SpawnGroup.MISC),
            new RegistryEntryGroup<>(SegmentedText.literal("Misc"), type -> type != EntityType.PLAYER
                    && type.getSpawnGroup() == SpawnGroup.MISC));

    public EntityTypeListSetting(CharSequence label) {
        super(label, () -> Registries.ENTITY_TYPE);
    }

    public EntityTypeListSetting(CharSequence label, Predicate<Object> visibleWhen) {
        super(label, () -> Registries.ENTITY_TYPE, visibleWhen);
    }

    public boolean accepts(Entity entity) {
        if (entity == null) return false;
        if (getSelectedCount() == 0) return true;
        return isEntrySelected(entity.getType());
    }

    @Override public List<RegistryEntryGroup<EntityType<?>>> getGroups() { return GROUPS; }
    @Override public Function<EntityType<?>, String> getEntryLabel() { return type -> type.getName().getString(); }
    @Override public String getTypeId() { return "ent"; }
}
