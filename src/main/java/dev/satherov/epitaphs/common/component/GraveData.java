package dev.satherov.epitaphs.common.component;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

import com.mojang.serialization.Codec;
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
        return new GraveData(AttachmentMigration.EMPTY_UUID, Instant.EPOCH, "Unknown");
    }
    
    public GraveData(ServerPlayer player, Instant timestamp) {
        this(player.getUUID(), timestamp, player.getGameProfile().getName());
    }
    
    public static final Codec<GraveData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AttachmentMigration.UUID_CODEC.fieldOf("uuid").forGetter(GraveData::owner),
            AttachmentMigration.TIMESTAMP_CODEC.fieldOf("timestamp").forGetter(GraveData::timestamp),
            Codec.STRING.optionalFieldOf("name", "Unknown").forGetter(GraveData::name)
    ).apply(instance, GraveData::new));
    
    public static final StreamCodec<ByteBuf, GraveData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, data -> data.owner().toString(),
            ByteBufCodecs.STRING_UTF8, data -> data.timestamp().toString(),
            ByteBufCodecs.STRING_UTF8, GraveData::name,
            (o, i, n) -> new GraveData(UUID.fromString(o), Instant.parse(i), n)
    );
}
