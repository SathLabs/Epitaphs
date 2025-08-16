package dev.satherov.epitaphs.common.block;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.common.component.EPGraveDataAttachment;
import dev.satherov.epitaphs.common.data.BackupHandler;
import dev.satherov.epitaphs.common.tile.GraveBlockEntity;
import dev.satherov.epitaphs.core.EPRegistry;
import dev.satherov.epitaphs.core.annotations.NothingNull;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@NothingNull
public class GraveBlock extends Block implements EntityBlock, SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private final VoxelShape SHAPE = Shapes.or(
            box(0, 0, 0, 16, 2, 16), // Ground
            box(1, 2, 0, 15, 4, 16), // Plate
            box(1, 2, 0, 15, 14, 2), // Stone
            box(2, 14, 0, 14, 16, 2) // Top
    );

    public GraveBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
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
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new GraveBlockEntity(blockPos, blockState);
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!(world instanceof ServerLevel level)) return;
        this.cleanup(level, pos, true);
        super.onRemove(state, world, pos, newState, isMoving);
    }
    
    public void cleanup(ServerLevel level, BlockPos pos, boolean drop) {
        if (!(level.getBlockEntity(pos) instanceof GraveBlockEntity grave)) return;
        MinecraftServer server = level.getServer();

        EPGraveDataAttachment data = grave.getData(EPRegistry.GRAVE_DATA);

        String uuid = data.getOwner();
        String timestamp = data.getTimestamp();
        if (uuid.isBlank() || timestamp.isBlank()) {
            Epitaphs.LOGGER.debug("Grave at '{}' has incomplete data, skipping cleanup", grave.getBlockPos());
            return;
        }

        Path storage = server.getWorldPath(LevelResource.ROOT)
                .normalize()
                .toAbsolutePath()
                .resolve("data")
                .resolve(Epitaphs.MOD_ID)
                .resolve(uuid);

        try {
            Path death = storage.resolve(timestamp + "-death.dat");
            Files.move(
                    death,
                    storage.resolve(timestamp + "-death.dat-old"),
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException e) {
            Epitaphs.LOGGER.error("Failed to rename old death data for '{}'", uuid, e);
        }

        if (drop) {
            List<ItemStack> contents = BackupHandler.getContents(server, uuid, timestamp);
            for (ItemStack stack : contents) {
                if (stack.isEmpty()) continue;
                ItemEntity entity = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                level.addFreshEntity(entity);
            }
        }
    }

    public static Optional<BlockPos> findSafeSpot(ServerLevel level, BlockPos grave) {
        BlockPos origin = new BlockPos(
                grave.getX(),
                Mth.clamp(grave.getY(), level.getMinBuildHeight() + 1, level.getMaxBuildHeight()),
                grave.getZ()
        ).immutable();

        List<BlockPos> stableCandidates = new ArrayList<>();
        List<BlockPos> fallbackCandidates = new ArrayList<>();

        level.getChunk(origin).findBlocks(
                state -> state.is(BlockTags.REPLACEABLE),
                (state, pos) -> state.is(BlockTags.REPLACEABLE),
                (pos, state) -> {
                    final BlockPos candidate = pos.immutable();
                    if (!level.getBlockState(pos.below()).is(BlockTags.REPLACEABLE)) stableCandidates.add(candidate);
                    else fallbackCandidates.add(candidate);
                }
        );

        Optional<BlockPos> stableResult = stableCandidates.stream().min(Comparator.comparingDouble(origin::distSqr));
        if (stableResult.isPresent()) return stableResult;

        return fallbackCandidates.stream().min(Comparator.comparingDouble(origin::distSqr));
    }
}