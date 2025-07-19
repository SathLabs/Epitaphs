package dev.satherov.epitaphs.common.tile;

import dev.satherov.epitaphs.core.EPRegistry;
import dev.satherov.epitaphs.core.annotations.NothingNull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@NothingNull
public class GraveBlockEntity extends BlockEntity {

    public GraveBlockEntity(BlockPos pos, BlockState blockState) {
        super(EPRegistry.GRAVE_BLOCK_ENTITY.get(), pos, blockState);
    }
}
