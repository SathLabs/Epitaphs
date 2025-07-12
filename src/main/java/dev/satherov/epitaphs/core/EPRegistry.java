package dev.satherov.epitaphs.core;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.common.block.EPGraveBlock;
import dev.satherov.epitaphs.common.tile.EPGraveBlockEntity;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

public class EPRegistry {

    public static DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Epitaphs.MOD_ID);
    public static DeferredRegister.Items ITEMS = DeferredRegister.createItems(Epitaphs.MOD_ID);
    public static DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Epitaphs.MOD_ID);

    public static DeferredHolder<Block, EPGraveBlock> GRAVE = register("grave", () -> new EPGraveBlock(BlockBehaviour.Properties.of()));

    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<EPGraveBlockEntity>> GRAVE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "grave_tile",
            () -> BlockEntityType.Builder.of(
                    EPGraveBlockEntity::new,
                    GRAVE.get()
            ).build(null)
    );

    public static <T extends Block> DeferredHolder<Block, T> register(String name, Supplier<? extends T> block) {
        DeferredHolder<Block, T> blockHolder = BLOCKS.register(name, block);
        ITEMS.registerSimpleBlockItem(blockHolder);
        return blockHolder;
    }
}
