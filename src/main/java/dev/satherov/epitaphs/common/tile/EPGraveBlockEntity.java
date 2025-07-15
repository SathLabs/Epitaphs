package dev.satherov.epitaphs.common.tile;

import dev.satherov.epitaphs.core.EPRegistry;
import dev.satherov.epitaphs.core.annotations.NothingNull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@NothingNull
public class EPGraveBlockEntity extends BlockEntity {

    private CompoundTag data = new CompoundTag();

    public EPGraveBlockEntity(BlockPos pos, BlockState blockState) {
        super(EPRegistry.GRAVE_BLOCK_ENTITY.get(), pos, blockState);
    }

    public CompoundTag getData() {
        return data;
    }

    public void saveData(CompoundTag data) {
        this.data = data;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        data = tag.getCompound("data");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("data", data);
    }
}
