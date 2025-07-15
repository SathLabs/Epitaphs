package dev.satherov.epitaphs.common.block;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.common.data.EPDataHandler;
import dev.satherov.epitaphs.common.data.EPSaveType;
import dev.satherov.epitaphs.common.tile.EPGraveBlockEntity;
import dev.satherov.epitaphs.core.annotations.NothingNull;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
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
public class EPGraveBlock extends Block implements EntityBlock, SimpleWaterloggedBlock {

    private final VoxelShape SHAPE = Shapes.or(
            box(0, 0, 0, 16, 2, 16), // Ground
            box(1, 2, 0, 15, 4, 16), // Plate
            box(1, 2, 0, 15, 14, 2), // Stone
            box(2, 14, 0, 14, 16, 2) // Top
    );

    public EPGraveBlock(Properties properties) {
        super(properties.strength(-1f, Float.MAX_VALUE));
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter blockGetter, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter blockGetter, BlockPos pos) {
        return 0.0F;
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new EPGraveBlockEntity(blockPos, blockState);
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!(world instanceof ServerLevel level)) return;
        if (!(level.getBlockEntity(pos) instanceof EPGraveBlockEntity grave)) return;

        CompoundTag data = grave.getData();
        if (data.isEmpty())
            super.onRemove(state, world, pos, newState, isMoving);

        String uuid = data.getString("uuid");
        String timestamp = data.getString("timestamp");
        if (uuid.isBlank() || timestamp.isBlank())
            super.onRemove(state, world, pos, newState, isMoving);

        Path dir = EPDataHandler.getDirectory(level, uuid).toAbsolutePath();
        Path src = dir.resolve(timestamp + "-death.dat");
        Path dest = dir.resolve(timestamp + "-death.dat-old");
        CompoundTag tag = EPDataHandler.load(level, EPSaveType.DEATH, data);

        List<ItemStack> contents = EPDataHandler.loadContents(level, tag);
        for (ItemStack stack : contents) {
            if (stack.isEmpty()) continue;
            ItemEntity entity = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            level.addFreshEntity(entity);
        }

        try {
            Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Epitaphs.LOGGER.error("Failed to rename {} to {}: {}", src, dest, e);
        }

        super.onRemove(state, world, pos, newState, isMoving);
    }

    public static Optional<BlockPos> findSafeSpot(ServerLevel level, BlockPos grave) {
        BlockPos origin = new BlockPos(
                grave.getX(),
                Mth.clamp(grave.getY(), level.getMinBuildHeight() + 1, level.getMaxBuildHeight()),
                grave.getZ()
        ).immutable();

        List<BlockPos> stableCandidates = new ArrayList<>();
        List<BlockPos> fallbackCandidates = new ArrayList<>();

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos chunkPos = origin.offset(x * 16, 0, z * 16);
                if (!level.getWorldBorder().isWithinBounds(chunkPos)) continue;

                level.getChunk(chunkPos).findBlocks(
                        state -> state.is(BlockTags.REPLACEABLE),
                        (state, pos) -> state.is(BlockTags.REPLACEABLE),
                        (pos, state) -> {
                            BlockPos immutablePos = pos.immutable();
                            boolean hasStableGround = !level.getBlockState(pos.below()).is(BlockTags.REPLACEABLE);

                            if (hasStableGround) stableCandidates.add(immutablePos);
                            else fallbackCandidates.add(immutablePos);

                        }
                );
            }
        }

        Optional<BlockPos> stableResult = stableCandidates.stream().min(Comparator.comparingDouble(origin::distSqr));
        if (stableResult.isPresent()) return stableResult;

        return fallbackCandidates.stream().min(Comparator.comparingDouble(origin::distSqr));
    }
}