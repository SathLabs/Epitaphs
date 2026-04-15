package dev.satherov.epitaphs.common.component;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.core.EPRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

///
/// Holds the data of all graves of a player.
///
/// @param positions A map of timestamps to the position of the grave.
///
public record LocationData(TreeMap<Instant, GlobalPos> positions) {
    private static final String ATTACHMENT_ID = Epitaphs.id("grave_locations").toString();
    
    public static LocationData empty() {
        return new LocationData(new TreeMap<>());
    }
    
    public static LocationData fromAttachments(CompoundTag attachments) {
        final Tag tag = attachments.get(LocationData.ATTACHMENT_ID);
        if (tag == null) return LocationData.empty();
        return LocationData.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow(IllegalStateException::new);
    }
    
    ///
    /// The actual codec. TODO: Rename this to CODEC after 1.21.1
    ///
    private static final Codec<LocationData> NEW = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(AttachmentMigration.TIMESTAMP_CODEC, GlobalPos.CODEC)
                    .xmap(TreeMap::new, Function.identity())
                    .fieldOf("positions")
                    .forGetter(LocationData::positions)
    ).apply(instance, LocationData::new));
    
    ///
    /// The old codec, used to convert the original format to the new format.
    /// TODO: Remove after 1.21.1
    ///
    @Deprecated(forRemoval = true)
    private static final Codec<LocationData> OLD = new Codec<>() {
        @Override
        public <T> DataResult<Pair<LocationData, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getList(input).flatMap(stream -> {
                TreeMap<Instant, GlobalPos> positions = new TreeMap<>();
                List<T> entries = new ArrayList<>();
                stream.accept(entries::add);
                
                for (T tag : entries) {
                    ops.getMap(tag).result().ifPresent(mapLike -> {
                        mapLike.entries().forEach(entry -> {
                            ops.getStringValue(entry.getFirst()).result().ifPresent(name -> {
                                ResourceKey<Level> dimension = ResourceKey.create(
                                        Registries.DIMENSION,
                                        ResourceLocation.parse(name)
                                );
                                
                                ops.getList(entry.getSecond()).result().ifPresent(posStream -> {
                                    List<T> list = new ArrayList<>();
                                    posStream.accept(list::add);
                                    
                                    for (T posTag : list) {
                                        ops.getMap(posTag).result().ifPresent(posMap -> {
                                            T timestamp = posMap.get("timestamp");
                                            T position = posMap.get("position");
                                            
                                            if (timestamp != null && position != null) {
                                                ops.getStringValue(timestamp).result()
                                                        .flatMap(instant -> AttachmentMigration.readTimestamp(instant).result())
                                                        .ifPresent(instant -> ops.getNumberValue(position).result().ifPresent(val -> {
                                                            positions.put(
                                                                    instant,
                                                                    GlobalPos.of(dimension, BlockPos.of(val.longValue()))
                                                            );
                                                        }));
                                            }
                                        });
                                    }
                                });
                            });
                        });
                    });
                }
                
                return DataResult.success(Pair.of(new LocationData(positions), input));
            });
        }
        
        @Override
        public <T> DataResult<T> encode(LocationData input, DynamicOps<T> ops, T prefix) {
            return DataResult.error(() -> "Do not use this codec!!!");
        }
    };
    
    public static final Codec<LocationData> CODEC = Codec.withAlternative(LocationData.NEW, LocationData.OLD);
    
    public static final StreamCodec<FriendlyByteBuf, LocationData> STREAM_CODEC = StreamCodec.of(
            (buf, val) -> {
                TreeMap<Instant, GlobalPos> positions = val.positions();
                buf.writeVarInt(positions.size());
                positions.forEach((instant, globalPos) -> {
                    buf.writeInstant(instant);
                    buf.writeGlobalPos(globalPos);
                });
            }, buf -> {
                TreeMap<Instant, GlobalPos> positions = new TreeMap<>();
                int size = buf.readVarInt();
                for (int i = 0; i < size; i++) {
                    Instant instant = buf.readInstant();
                    GlobalPos globalPos = buf.readGlobalPos();
                    positions.put(instant, globalPos);
                }
                return new LocationData(positions);
            }
    );
    
    ///
    /// Adds a new entry to the list.
    ///
    public LocationData add(Instant now, GlobalPos pos) {
        this.positions.put(now, pos);
        return this;
    }
    
    ///
    /// Removes an entry from the list.
    ///
    public LocationData remove(Instant now) {
        this.positions.remove(now);
        return this;
    }
    
    ///
    /// Gets the latest position the player died at.
    ///
    public Optional<GlobalPos> latest() {
        @Nullable Map.Entry<Instant, GlobalPos> entry = this.positions.lastEntry();
        if (entry == null) return Optional.empty();
        return Optional.of(entry.getValue());
    }
    
    ///
    /// Gets all positions of the given player.
    /// Will remove entries if they are not a grave anymore.
    ///
    /// @param server Server instance.
    ///
    /// @return Map of timestamps to the position of all graves.
    ///
    public TreeMap<Instant, GlobalPos> getAll(MinecraftServer server) {
        this.positions.entrySet().removeIf(entry -> {
            final GlobalPos global = entry.getValue();
            ServerLevel level = server.getLevel(global.dimension());
            if (level == null) return true;
            final BlockPos pos = global.pos();
            final BlockState state = level.getBlockState(pos);
            return level.isLoaded(pos) && !state.is(EPRegistry.GRAVE);
        });
        return this.positions;
    }
}
