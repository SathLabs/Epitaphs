package dev.satherov.epitaphs.common.block;

import dev.satherov.epitaphs.common.component.GraveData;
import dev.satherov.epitaphs.common.data.BackupType;
import dev.satherov.epitaphs.common.data.DataHandler;
import dev.satherov.epitaphs.core.EPRegistry;
import dev.satherov.epitaphs.core.annotations.NothingNull;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@NothingNull
public class GraveBlock extends Block implements EntityBlock, SimpleWaterloggedBlock {
    
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 2, 16), // Ground
            Block.box(1, 2, 0, 15, 4, 16), // Plate
            Block.box(1, 2, 0, 15, 14, 2), // Stone
            Block.box(2, 14, 0, 14, 16, 2)  // Top
    );
    
    public GraveBlock() {
        super(Properties.ofFullCopy(Blocks.BEDROCK));
        this.registerDefaultState(this.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GraveBlock.WATERLOGGED);
    }
    
    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(GraveBlock.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
    
    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter blockGetter, BlockPos pos, Player player) {
        return player.isCreative();
    }
    
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter blockGetter, BlockPos pos) {
        return player.isCreative() ? super.getDestroyProgress(state, player, blockGetter, pos) : 0.0F;
    }
    
    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return Float.MAX_VALUE;
    }
    
    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        // DONT TOUCH ME >:c
    }
    
    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return false;
    }
    
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return GraveBlock.SHAPE;
    }
    
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GraveBlockEntity(pos, state);
    }
    
    ///
    /// Attempt to drop all items within the saved file when the grave is broken.
    ///
    @Override
    protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() == newState.getBlock()) return;
        if (!(world instanceof ServerLevel level)) return;
        if (!(level.getBlockEntity(pos) instanceof GraveBlockEntity entity)) return;
        MinecraftServer server = level.getServer();
        GraveData data = entity.getData(EPRegistry.GRAVE_DATA);
        UUID uuid = data.owner();
        Instant timestamp = data.timestamp();
        List<ItemStack> items = DataHandler.gather(server, uuid, timestamp, BackupType.DEATH);
        items.forEach(stack -> Block.popResource(level, pos, stack));
        DataHandler.invalidate(server, uuid, timestamp);
    }
    
    ///
    /// Finds a safe spot for the grave to be placed. Snakes outwards from the given chunk
    /// and will try to find an unoccupied spot with solid ground or a block where dirt can be spawned below
    ///
    /// @param level The level to search in.
    /// @param death The position of the player's death and start position to search.
    ///
    public static BlockPos findSafeSpot(ServerLevel level, BlockPos death) {
        BlockPos origin = new BlockPos(
                death.getX(),
                Mth.clamp(death.getY(), level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 1),
                death.getZ()
        ).immutable();
        
        List<BlockPos> candidates = new ArrayList<>();
        List<BlockPos> fallbacks = new ArrayList<>();
        level.getChunk(origin).findBlocks(
                state -> state.isAir() || state.is(BlockTags.REPLACEABLE),
                (pos, state) -> {
                    final BlockPos candidate = pos.immutable();
                    BlockState below = level.getBlockState(candidate.below());
                    if (!level.isInWorldBounds(candidate) || !level.isInWorldBounds(candidate.below())) return;
                    if (!(below.isAir() || below.is(BlockTags.REPLACEABLE))) candidates.add(candidate);
                    else fallbacks.add(candidate);
                }
        );
        
        return candidates.stream()
                .min(Comparator.comparingDouble(origin::distSqr))
                .orElse(fallbacks.stream()
                        .min(Comparator.comparingDouble(origin::distSqr))
                        .orElseGet(origin::immutable)
                );
    }
}
