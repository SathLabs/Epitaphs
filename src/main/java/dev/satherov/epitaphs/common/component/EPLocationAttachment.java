package dev.satherov.epitaphs.common.component;

import dev.satherov.epitaphs.core.EPRegistry;

import net.neoforged.neoforge.common.util.INBTSerializable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.UnknownNullability;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

public class EPLocationAttachment implements INBTSerializable<ListTag> {

    private final Map<String, List<Entry<String, BlockPos>>> graveLocations = new HashMap<>();

    public Map<String, List<Entry<String, BlockPos>>> getGraveLocations(ServerLevel level) {
        clearMissing(level);
        return graveLocations;
    }

    public EPLocationAttachment addGraveLocation(ServerPlayer player, String timestamp, BlockPos pos) {
        graveLocations.computeIfAbsent(player.level().dimension().location().toString(), k -> new ArrayList<>())
                .add(new AbstractMap.SimpleEntry<>(timestamp, pos.immutable()));
        clearMissing(player.serverLevel());
        return this;
    }

    public EPLocationAttachment removeGraveLocation(String timestamp, ResourceKey<Level> dimension, BlockPos pos) {
        graveLocations.computeIfAbsent(dimension.location().toString(), k -> new ArrayList<>())
                .removeIf(entry -> entry.getKey().equals(timestamp) && entry.getValue().equals(pos.immutable()));
        return this;
    }

    public EPLocationAttachment removeGraveLocation(ServerPlayer player, String timestamp, BlockPos pos) {
        removeGraveLocation(timestamp, player.level().dimension(), pos);
        clearMissing(player.serverLevel());
        return this;
    }

    public Optional<GlobalPos> findLatestGraveLocation(ServerLevel level) {
        clearMissing(level);
        return graveLocations.entrySet().stream()
                .flatMap(dimension -> dimension.getValue()
                        .stream()
                        .map(entry -> new AbstractMap.SimpleEntry<>(
                                entry.getKey(),
                                GlobalPos.of(
                                        ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimension.getKey())),
                                        entry.getValue()
                                )
                        )))
                .max(Entry.comparingByKey())
                .map(Entry::getValue);
    }

    private void clearMissing(ServerLevel level) {
        graveLocations.entrySet().removeIf(dimensionEntry -> {
            if (!level.dimension().location().equals(ResourceLocation.parse(dimensionEntry.getKey()))) return false;
            List<Entry<String, BlockPos>> list = dimensionEntry.getValue();
        
            list.removeIf(entry -> {
                BlockPos pos = entry.getValue();
                return level.isLoaded(pos) && !level.getBlockState(pos).is(EPRegistry.GRAVE.get());
            });
        
            return list.isEmpty();
        });
    }

    @Override
    public @UnknownNullability ListTag serializeNBT(HolderLookup.Provider provider) {
        ListTag dimensions = new ListTag();
        graveLocations.forEach((key, value) -> {
            CompoundTag dimension = new CompoundTag();
            ListTag positions = new ListTag();
            value.forEach(entry -> {
                CompoundTag tag = new CompoundTag();
                tag.putString("timestamp", entry.getKey());
                tag.putLong("position", entry.getValue().asLong());
                positions.add(tag);
            });
            dimension.put(key, positions);
            dimensions.add(dimension);
        });
        return dimensions;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, ListTag nbt) {
        Map<String, List<Entry<String, BlockPos>>> graveLocations = new HashMap<>();

        for (int i = 0; i < nbt.size(); i++) {
            CompoundTag tag = nbt.getCompound(i);
            tag.getAllKeys().forEach(dimension -> {
                ListTag positions = tag.getList(dimension, Tag.TAG_COMPOUND);
                List<Entry<String, BlockPos>> entries = new ArrayList<>();
                for (int j = 0; j < positions.size(); j++) {
                    CompoundTag position = positions.getCompound(j);
                    entries.add(new AbstractMap.SimpleEntry<>(
                            position.getString("timestamp"),
                            BlockPos.of(position.getLong("position"))
                    ));
                }
                graveLocations.put(dimension, entries);
            });
        }
        this.graveLocations.putAll(graveLocations);
    }
}