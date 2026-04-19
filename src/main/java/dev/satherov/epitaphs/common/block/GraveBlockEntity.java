package dev.satherov.epitaphs.common.block;

import dev.satherov.epitaphs.core.EPRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GraveBlockEntity extends BlockEntity {
    
    public GraveBlockEntity(BlockPos pos, BlockState state) {
        super(EPRegistry.GRAVE_BLOCK_ENTITY.get(), pos, state);
    }
}
