package dev.satherov.epitaphs.core;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.common.block.GraveBlock;
import dev.satherov.epitaphs.common.block.GraveBlockEntity;
import dev.satherov.epitaphs.common.component.GraveData;
import dev.satherov.epitaphs.common.component.LocationData;
import dev.satherov.epitaphs.common.component.SoulBottleConsumeEffect;
import dev.satherov.epitaphs.common.component.SoulboundData;
import dev.satherov.epitaphs.common.component.TrackedLocation;
import dev.satherov.epitaphs.common.item.SoulBottleItem;
import dev.satherov.sathlib.common.item.SLItemProperties;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.mojang.serialization.Codec;

import java.util.function.Function;
import java.util.function.Supplier;

public final class EPRegistry {
    
    public static final FoodProperties SOUL_BOTTLE_FOOD = new FoodProperties.Builder().alwaysEdible().build();
    public static final Consumable SOUL_BOTTLE_CONSUMABLE = Consumables.defaultDrink().consumeSeconds(2.6F).onConsume(SoulBottleConsumeEffect.INSTANCE).build();
    public static final TagKey<Item> SOULBOUND_ENCHANTABLE = TagKey.create(Registries.ITEM, Epitaphs.neo("enchantable/soulbound"));
    public static final TagKey<Item> EXPERIENCE_SOULBOUND_ENCHANTABLE = TagKey.create(Registries.ITEM, Epitaphs.neo("enchantable/experience_soulbound"));
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Epitaphs.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Epitaphs.MOD_ID);
    public static final DeferredHolder<Block, GraveBlock> GRAVE = EPRegistry.register("grave", GraveBlock::new);
    public static final DeferredHolder<Item, SoulBottleItem> SOUL_BOTTLE = EPRegistry.ITEMS.register("soul_bottle", k -> new SoulBottleItem(SLItemProperties.create(k).food(EPRegistry.SOUL_BOTTLE_FOOD, EPRegistry.SOUL_BOTTLE_CONSUMABLE)));
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Epitaphs.MOD_ID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Epitaphs.MOD_ID);
    public static final Supplier<AttachmentType<LocationData>> LOCATION_DATA = EPRegistry.ATTACHMENT_TYPES.register(
            "grave_locations",
            () -> AttachmentType.builder(LocationData::empty)
                    .serialize(LocationData.CODEC)
                    .sync(LocationData.STREAM_CODEC)
                    .copyOnDeath()
                    .build()
    );    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GraveBlockEntity>> GRAVE_BLOCK_ENTITY = EPRegistry.BLOCK_ENTITY_TYPES.register(
            "grave_tile",
            () -> new BlockEntityType<>(
                    GraveBlockEntity::new,
                    EPRegistry.GRAVE.get()
            )
    );
    public static final Supplier<AttachmentType<GraveData>> GRAVE_DATA = EPRegistry.ATTACHMENT_TYPES.register(
            "grave_data",
            () -> AttachmentType.builder(GraveData::empty)
                    .serialize(GraveData.CODEC)
                    .sync(GraveData.STREAM_CODEC)
                    .build()
    );
    public static final Supplier<AttachmentType<TrackedLocation>> TRACKED_LOCATION_DATA = EPRegistry.ATTACHMENT_TYPES.register(
            "tracked_location_data",
            () -> AttachmentType.builder(() -> TrackedLocation.ZERO)
                    .serialize(TrackedLocation.CODEC)
                    .sync(TrackedLocation.STREAM_CODEC)
                    .copyOnDeath()
                    .build()
    );
    public static final Supplier<AttachmentType<SoulboundData>> SOULBOUND_DATA = EPRegistry.ATTACHMENT_TYPES.register(
            "soulbound_data",
            () -> AttachmentType.builder(SoulboundData::empty)
                    .serialize(SoulboundData.CODEC)
                    .copyOnDeath()
                    .build()
    );
    private static final DeferredRegister<DataComponentType<?>> ENCHANTMENT_DATA_COMPONENTS = DeferredRegister.create(BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE, Epitaphs.MOD_ID);
    public static final Supplier<DataComponentType<Boolean>> SOULBOUND = EPRegistry.ENCHANTMENT_DATA_COMPONENTS.register(
            "soulbound",
            () -> DataComponentType.<Boolean>builder()
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .persistent(Codec.BOOL)
                    .build()
    );
    
    public static final Supplier<DataComponentType<Boolean>> EXPERIENCE_SOULBOUND = EPRegistry.ENCHANTMENT_DATA_COMPONENTS.register(
            "experience_soulbound",
            () -> DataComponentType.<Boolean>builder()
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .persistent(Codec.BOOL)
                    .build()
    );
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, Epitaphs.MOD_ID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = EPRegistry.TABS.register("epitaphs", () -> CreativeModeTab.builder()
            .title(Component.translatable("creative_tab.epitaphs"))
            .icon(() -> EPRegistry.GRAVE.get().asItem().getDefaultInstance())
            .displayItems((_, output) -> {
                output.accept(EPRegistry.GRAVE.get());
                output.accept(EPRegistry.SOUL_BOTTLE.get());
            })
            .build()
    );
    
    private static <T extends Block> DeferredHolder<Block, T> register(String name, Function<Identifier, ? extends T> block) {
        final DeferredHolder<Block, T> holder = EPRegistry.BLOCKS.register(name, block);
        EPRegistry.ITEMS.registerSimpleBlockItem(holder);
        return holder;
    }
    
    public static void register(final IEventBus bus) {
        EPRegistry.BLOCKS.register(bus);
        EPRegistry.ITEMS.register(bus);
        EPRegistry.BLOCK_ENTITY_TYPES.register(bus);
        EPRegistry.ATTACHMENT_TYPES.register(bus);
        EPRegistry.ENCHANTMENT_DATA_COMPONENTS.register(bus);
        EPRegistry.TABS.register(bus);
    }
    

}
