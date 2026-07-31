package dev.satherov.epitaphs.common.menu;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record PreviewData(
        PreviewData.Profile profile,
        PreviewData.Vitals vitals,
        PreviewData.Location location,
        int selectedSlot,
        List<PreviewData.CurioSection> curios,
        List<String> backups
) {
    
    public static final Codec<PreviewData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PreviewData.Profile.CODEC.fieldOf("profile").forGetter(PreviewData::profile),
            PreviewData.Vitals.CODEC.fieldOf("vitals").forGetter(PreviewData::vitals),
            PreviewData.Location.CODEC.fieldOf("location").forGetter(PreviewData::location),
            Codec.INT.optionalFieldOf("selected_slot", 0).forGetter(PreviewData::selectedSlot),
            PreviewData.CurioSection.CODEC.listOf().optionalFieldOf("curios", List.of()).forGetter(PreviewData::curios),
            Codec.STRING.listOf().optionalFieldOf("backups", List.of()).forGetter(PreviewData::backups)
    ).apply(instance, PreviewData::new));
    
    public static final StreamCodec<ByteBuf, PreviewData> STREAM_CODEC = ByteBufCodecs.fromCodec(PreviewData.CODEC);
    
    public record Profile(UUID uuid, String name, long timestamp, String file) {
        
        public static final Codec<PreviewData.Profile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("uuid").forGetter(PreviewData.Profile::uuid),
                Codec.STRING.fieldOf("name").forGetter(PreviewData.Profile::name),
                Codec.LONG.fieldOf("timestamp").forGetter(PreviewData.Profile::timestamp),
                Codec.STRING.fieldOf("file").forGetter(PreviewData.Profile::file)
        ).apply(instance, PreviewData.Profile::new));
    }
    
    public record Vitals(
            float health,
            float maxHealth,
            float absorption,
            int food,
            float saturation,
            int experienceLevel,
            float experienceProgress,
            int experienceTotal
    ) {
        
        public static final Codec<PreviewData.Vitals> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("health").forGetter(PreviewData.Vitals::health),
                Codec.FLOAT.fieldOf("max_health").forGetter(PreviewData.Vitals::maxHealth),
                Codec.FLOAT.fieldOf("absorption").forGetter(PreviewData.Vitals::absorption),
                Codec.INT.fieldOf("food").forGetter(PreviewData.Vitals::food),
                Codec.FLOAT.fieldOf("saturation").forGetter(PreviewData.Vitals::saturation),
                Codec.INT.fieldOf("experience_level").forGetter(PreviewData.Vitals::experienceLevel),
                Codec.FLOAT.fieldOf("experience_progress").forGetter(PreviewData.Vitals::experienceProgress),
                Codec.INT.fieldOf("experience_total").forGetter(PreviewData.Vitals::experienceTotal)
        ).apply(instance, PreviewData.Vitals::new));
    }
    
    public record Location(
            Optional<ResourceKey<Level>> dimension,
            Vec3 position,
            Vec2 rotation,
            int air,
            int score,
            GameType gameType
    ) {
        
        public static final Codec<PreviewData.Location> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Level.RESOURCE_KEY_CODEC.optionalFieldOf("dimension").forGetter(PreviewData.Location::dimension),
                Vec3.CODEC.fieldOf("position").forGetter(PreviewData.Location::position),
                Vec2.CODEC.fieldOf("rotation").forGetter(PreviewData.Location::rotation),
                Codec.INT.fieldOf("air").forGetter(PreviewData.Location::air),
                Codec.INT.fieldOf("score").forGetter(PreviewData.Location::score),
                GameType.CODEC.fieldOf("game_type").forGetter(PreviewData.Location::gameType)
        ).apply(instance, PreviewData.Location::new));
    }
    
    public record CurioSection(String identifier, int items, int cosmetics) {
        
        public static final Codec<PreviewData.CurioSection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("identifier").forGetter(PreviewData.CurioSection::identifier),
                Codec.INT.fieldOf("items").forGetter(PreviewData.CurioSection::items),
                Codec.INT.fieldOf("cosmetics").forGetter(PreviewData.CurioSection::cosmetics)
        ).apply(instance, PreviewData.CurioSection::new));
        
        public int size() {
            return this.items + this.cosmetics;
        }
    }
}
