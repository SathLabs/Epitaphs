package dev.satherov.epitaphs.common.block;

import dev.satherov.epitaphs.common.tile.EPGraveBlockEntity;
import dev.satherov.epitaphs.core.annotations.NothingNull;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

@NothingNull
public class EPGraveBlock extends Block implements EntityBlock {

    public EPGraveBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new EPGraveBlockEntity(blockPos, blockState);
    }
}
