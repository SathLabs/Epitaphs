package dev.satherov.epitaphs.common.component;

import dev.satherov.epitaphs.common.container.PlayerContainer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

///
/// Holds the soulbound data of a player to persist through death.
///
public record SoulboundData(PlayerContainer container, int experience) {
    
    public static SoulboundData empty() {
        return new SoulboundData(PlayerContainer.empty(), 0);
    }
    
    public static final MapCodec<SoulboundData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            PlayerContainer.CODEC.fieldOf("container").forGetter(SoulboundData::container),
            Codec.INT.fieldOf("experience").forGetter(SoulboundData::experience)
    ).apply(instance, SoulboundData::new));
    
    ///
    /// Checks if this {@link SoulboundData} is empty.
    ///
    /// @return {@code true} if this soulbound data is empty.
    ///
    public boolean isEmpty() {
        return this.experience <= 0 && this.container.isEmpty();
    }
}
