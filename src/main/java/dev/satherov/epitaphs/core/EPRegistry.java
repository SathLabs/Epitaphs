package dev.satherov.epitaphs.core;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.common.block.GraveBlock;
import dev.satherov.epitaphs.common.component.EPGraveDataAttachment;
import dev.satherov.epitaphs.common.component.EPLocationAttachment;
import dev.satherov.epitaphs.common.component.EPSoulboundAttachment;
import dev.satherov.epitaphs.common.tile.GraveBlockEntity;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import com.mojang.serialization.Codec;

import java.util.function.Supplier;

public class EPRegistry {
    
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Epitaphs.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Epitaphs.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Epitaphs.MOD_ID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Epitaphs.MOD_ID);
    public static final DeferredRegister<DataComponentType<?>> ENCHANTMENT_DATA_COMPONENTS = DeferredRegister.create(BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE, Epitaphs.MOD_ID);
    
    public static final DeferredHolder<Block, GraveBlock> GRAVE = register("grave", () -> new GraveBlock(BlockBehaviour.Properties.of()));
    
    public static <T extends Block> DeferredHolder<Block, T> register(String name, Supplier<? extends T> block) {
        DeferredHolder<Block, T> blockHolder = BLOCKS.register(name, block);
        ITEMS.registerSimpleBlockItem(blockHolder);
        return blockHolder;
    }
    
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GraveBlockEntity>> GRAVE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "grave_tile",
            () -> BlockEntityType.Builder.of(
                    GraveBlockEntity::new,
                    GRAVE.get()
            ).build(null)
    );
    
    public static final Supplier<AttachmentType<EPLocationAttachment>> LOCATION_DATA = ATTACHMENT_TYPES.register(
            "grave_locations",
            () -> AttachmentType
                    .serializable(EPLocationAttachment::new)
                    .copyOnDeath()
                    .build()
    );
    
    public static final Supplier<AttachmentType<EPGraveDataAttachment>> GRAVE_DATA = ATTACHMENT_TYPES.register(
            "grave_data",
            () -> AttachmentType
                    .serializable(EPGraveDataAttachment::new)
                    .build()
    );
    
    public static final Supplier<AttachmentType<EPSoulboundAttachment>> SOULBOUND_DATA = ATTACHMENT_TYPES.register(
            "soulbound_data",
            () -> AttachmentType
                    .serializable(EPSoulboundAttachment::new)
                    .copyOnDeath()
                    .build()
    );
    
    public static final Supplier<DataComponentType<Boolean>> SOULBOUND = ENCHANTMENT_DATA_COMPONENTS.register(
            "soulbound",
            () -> DataComponentType.<Boolean>builder()
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .persistent(Codec.BOOL)
                    .build()
    );
    
    public static final Supplier<DataComponentType<Boolean>> EXPERIENCE_SOULBOUND = ENCHANTMENT_DATA_COMPONENTS.register(
            "experience_soulbound",
            () -> DataComponentType.<Boolean>builder()
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .persistent(Codec.BOOL)
                    .build()
    );
    
    public static final TagKey<Item> SOULBOUND_ENCHANTABLE = TagKey.create(Registries.ITEM, Epitaphs.neo("enchantable/soulbound"));
    public static final TagKey<Item> EXPERIENCE_SOULBOUND_ENCHANTABLE = TagKey.create(Registries.ITEM, Epitaphs.neo("enchantable/experience_soulbound"));
}
