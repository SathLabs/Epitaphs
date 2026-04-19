package dev.satherov.epitaphs.core.mixin;

import dev.satherov.epitaphs.core.EPRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    
    @Inject(
            method = "mayInteract",
            at = @At("RETURN"),
            cancellable = true
    )
    public void mayInteract(Entity entity, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (((ServerLevel) (Object) this).getBlockState(pos).is(EPRegistry.GRAVE.get())) cir.setReturnValue(true);
    }
}
