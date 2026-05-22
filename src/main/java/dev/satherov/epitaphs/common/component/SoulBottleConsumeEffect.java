package dev.satherov.epitaphs.common.component;

import dev.satherov.epitaphs.core.EPRegistry;
import dev.satherov.sathlib.core.annotations.NothingNull;

import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;

import com.mojang.serialization.MapCodec;

import java.time.Instant;
import java.util.Set;
import java.util.TreeMap;

@NothingNull
public enum SoulBottleConsumeEffect implements ConsumeEffect {
    INSTANCE;
    
    public static final MapCodec<SoulBottleConsumeEffect> CODEC = MapCodec.unit(SoulBottleConsumeEffect.INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, SoulBottleConsumeEffect> STREAM_CODEC = StreamCodec.unit(SoulBottleConsumeEffect.INSTANCE);
    public static final Type<SoulBottleConsumeEffect> TYPE = new Type<>(SoulBottleConsumeEffect.CODEC, SoulBottleConsumeEffect.STREAM_CODEC);
    
    private static Set<Relative> getRelatives(BlockPos blockPos, boolean sameDimension) {
        final Coordinates destination = WorldCoordinates.absolute(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        Set<Relative> dir = Relative.direction(destination.isXRelative(), destination.isYRelative(), destination.isZRelative());
        Set<Relative> pos = sameDimension ? Relative.position(destination.isXRelative(), destination.isYRelative(), destination.isZRelative()) : Set.of();
        Set<Relative> rot = Relative.ROTATION;
        return Relative.union(dir, pos, rot);
    }
    
    @Override
    public Type<SoulBottleConsumeEffect> getType() {
        return SoulBottleConsumeEffect.TYPE;
    }
    
    @Override
    public boolean apply(Level level, ItemStack stack, LivingEntity user) {
        if (!user.hasData(EPRegistry.LOCATION_DATA)) return false;
        LocationData data = user.getData(EPRegistry.LOCATION_DATA);
        TreeMap<Instant, GlobalPos> positions = data.positions();
        if (positions.isEmpty()) return true;
        
        final GlobalPos global = positions.lastEntry().getValue();
        final BlockPos pos = global.pos();
        
        if (level instanceof ServerLevel serverLevel) {
            MinecraftServer server = serverLevel.getServer();
            ServerLevel target = server.getLevel(global.dimension());
            if (target == null) return false;
            
            level.levelEvent(LevelEvent.PARTICLES_EYE_OF_ENDER_DEATH, user.blockPosition(), 0);
            Set<Relative> relatives = SoulBottleConsumeEffect.getRelatives(pos, global.dimension() == user.level().dimension());
            user.teleportTo(target, pos.getX(), pos.getY(), pos.getZ(), relatives, user.getYRot(), user.getXRot(), false);
            level.playSound(user, pos, SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        } else {
            user.playSound(SoundEvents.PLAYER_TELEPORT);
        }
        
        return true;
    }
}
