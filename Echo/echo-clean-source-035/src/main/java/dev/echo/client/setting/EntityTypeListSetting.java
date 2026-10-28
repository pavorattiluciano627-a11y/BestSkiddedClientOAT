package dev.echo.client.setting;

import dev.echo.client.setting.list.RegistryEntryGroup;
import dev.echo.client.text.SegmentedText;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;

/** Entity-type selection with the original four preset groups. */
public class EntityTypeListSetting extends RegistryListSetting<EntityType<?>> {
    private static final List<RegistryEntryGroup<EntityType<?>>> GROUPS = List.of(
            new RegistryEntryGroup<>(SegmentedText.literal("Players"), type -> type == EntityTypes.PLAYER),
            new RegistryEntryGroup<>(SegmentedText.literal("Hostile"), type -> type.getCategory() == MobCategory.MONSTER),
            new RegistryEntryGroup<>(SegmentedText.literal("Passive"), type -> type != EntityTypes.PLAYER
                    && type.getCategory() != MobCategory.MONSTER && type.getCategory() != MobCategory.MISC),
            new RegistryEntryGroup<>(SegmentedText.literal("Misc"), type -> type != EntityTypes.PLAYER
                    && type.getCategory() == MobCategory.MISC));

    public EntityTypeListSetting(CharSequence label) {
        super(label, () -> BuiltInRegistries.ENTITY_TYPE);
    }

    public EntityTypeListSetting(CharSequence label, Predicate<Object> visibleWhen) {
        super(label, () -> BuiltInRegistries.ENTITY_TYPE, visibleWhen);
    }

    public boolean accepts(Entity entity) {
        if (entity == null) return false;
        if (getSelectedCount() == 0) return true;
        return isEntrySelected(entity.getType());
    }

    @Override public List<RegistryEntryGroup<EntityType<?>>> getGroups() { return GROUPS; }
    @Override public Function<EntityType<?>, String> getEntryLabel() { return type -> type.getDescription().getString(); }
    @Override public String getTypeId() { return "ent"; }
}
