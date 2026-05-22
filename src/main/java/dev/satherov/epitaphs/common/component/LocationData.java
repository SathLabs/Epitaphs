package dev.satherov.epitaphs.common.component;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.core.EPRegistry;
import dev.satherov.sathlib.network.codec.SLCodec;
import dev.satherov.sathlib.network.codec.SLStreamCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import java.time.Instant;
import java.util.TreeMap;

///
/// Holds the data of all graves of a player.
///
/// @param positions A map of timestamps to the position of the grave.
///
public record LocationData(TreeMap<Instant, GlobalPos> positions) {
    
    public static final MapCodec<LocationData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SLCodec.map(SLCodec.INSTANT, GlobalPos.CODEC, TreeMap::new).fieldOf("positions").forGetter(LocationData::positions)
    ).apply(instance, LocationData::new));
    public static final StreamCodec<ByteBuf, LocationData> STREAM_CODEC = SLStreamCodec.map(SLStreamCodec.INSTANT, GlobalPos.STREAM_CODEC, TreeMap::new).map(LocationData::new, LocationData::positions);
    private static final String ATTACHMENT_ID = Epitaphs.id("grave_locations").toString();
    
    public static LocationData empty() {
        return new LocationData(new TreeMap<>());
    }
    
    public static LocationData fromAttachments(CompoundTag attachments) {
        final Tag tag = attachments.get(LocationData.ATTACHMENT_ID);
        if (tag == null) return LocationData.empty();
        return LocationData.CODEC.codec().parse(NbtOps.INSTANCE, tag).getOrThrow(IllegalStateException::new);
    }
    
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
