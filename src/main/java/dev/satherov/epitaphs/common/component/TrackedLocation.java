package dev.satherov.epitaphs.common.component;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;

import com.mojang.serialization.MapCodec;

import io.netty.buffer.ByteBuf;

public record TrackedLocation(GlobalPos pos) {
    
    public static final TrackedLocation ZERO = new TrackedLocation(new GlobalPos(Level.OVERWORLD, BlockPos.ZERO));
    
    public static final MapCodec<TrackedLocation> CODEC = GlobalPos.MAP_CODEC.xmap(TrackedLocation::new, TrackedLocation::pos);
    
    public static final StreamCodec<ByteBuf, TrackedLocation> STREAM_CODEC = GlobalPos.STREAM_CODEC.map(TrackedLocation::new, TrackedLocation::pos);
    
    public boolean isEmpty() {
        return this.equals(TrackedLocation.ZERO);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof TrackedLocation(GlobalPos other))) return false;
        return this.pos.dimension().equals(other.dimension()) && this.pos.equals(other);
    }
}
