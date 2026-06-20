package dev.satherov.epitaphs.common.block;

import dev.satherov.epitaphs.EPConfig;
import dev.satherov.epitaphs.core.EPRegistry;
import dev.satherov.sathlib.common.blockentity.AreaBlockEntity;
import dev.satherov.sathlib.util.AreaTracker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class GraveBlockEntity extends AreaBlockEntity<GraveBlockEntity> {
    
    public static final AreaTracker<GraveBlockEntity> TRACKER = AreaTracker.create();
    
    public GraveBlockEntity(BlockPos pos, BlockState state) {
        super(EPRegistry.GRAVE_BLOCK_ENTITY.get(), pos, state);
    }
    
    public static BoundingBox getVillagerDeathArea() {
        return new BoundingBox(BlockPos.ZERO).inflatedBy(EPConfig.Server.getVillagerDeathDistance());
    }
    
    @Override
    public AreaTracker<GraveBlockEntity> getTracker() {
        return GraveBlockEntity.TRACKER;
    }
    
    @Override
    public void onLoad() {
        super.onLoad();
        if (EPConfig.Server.getVillagerDeathDistance() < 1) return;
        if (!this.getBlockState().getValue(GraveBlock.SOULS)) {
            GraveBlockEntity.TRACKER.update(this.self(), GraveBlockEntity.getVillagerDeathArea());
        }
    }
    
    @Override
    public void setRemoved() {
        this.getTracker().unregister(this.self());
        super.setRemoved();
    }
}
