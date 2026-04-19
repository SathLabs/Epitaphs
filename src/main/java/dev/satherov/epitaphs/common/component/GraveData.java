package dev.satherov.epitaphs.common.component;

import dev.satherov.sathlib.network.codec.SLCodec;
import dev.satherov.sathlib.network.codec.SLStreamCodec;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import java.time.Instant;
import java.util.UUID;

///
/// Holds the data about a grave. Stored on the block entity.
///
/// @param owner     The UUID of the owner of the grave.
/// @param timestamp The timestamp of the grave's creation.
/// @param name      The name of the owner of the grave.
///
public record GraveData(UUID owner, Instant timestamp, String name) {
    
    public static GraveData empty() {
        return new GraveData(UUID.nameUUIDFromBytes(new byte[0]), Instant.EPOCH, "Unknown");
    }
    
    public GraveData(ServerPlayer player, Instant timestamp) {
        this(player.getUUID(), timestamp, player.getGameProfile().name());
    }
    
    public static final MapCodec<GraveData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SLCodec.UUID.fieldOf("uuid").forGetter(GraveData::owner),
            SLCodec.INSTANT.fieldOf("timestamp").forGetter(GraveData::timestamp),
            Codec.STRING.optionalFieldOf("name", "Unknown").forGetter(GraveData::name)
    ).apply(instance, GraveData::new));
    
    public static final StreamCodec<ByteBuf, GraveData> STREAM_CODEC = StreamCodec.composite(
            SLStreamCodec.UUID, GraveData::owner,
            SLStreamCodec.INSTANT, GraveData::timestamp,
            ByteBufCodecs.STRING_UTF8, GraveData::name,
            GraveData::new
    );
}
