package dev.satherov.epitaphs.datagen.data;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.core.EPRegistry;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public class EPEnchantments {

    public static final ResourceKey<Enchantment> SOULBOUND = createKey("soulbound");
    public static final ResourceKey<Enchantment> EXPERIENCE_SOULBOUND = createKey("experience_soulbound");

    public static void bootstrap(BootstrapContext<Enchantment> context) {
        HolderGetter<Item> holderGetter = context.lookup(Registries.ITEM);
        HolderGetter<Enchantment> enchantmentGetter = context.lookup(Registries.ENCHANTMENT);

        context.register(
                SOULBOUND,
                Enchantment.enchantment(
                                Enchantment.definition(
                                        holderGetter.getOrThrow(EPRegistry.SOULBOUND_ENCHANTABLE),
                                        holderGetter.getOrThrow(EPRegistry.SOULBOUND_ENCHANTABLE),
                                        6,
                                        1,
                                        Enchantment.dynamicCost(1, 0),
                                        Enchantment.dynamicCost(100, 0),
                                        0,
                                        EquipmentSlotGroup.ANY
                                )
                        )
                        .withSpecialEffect(EPRegistry.SOULBOUND.get(), false)
                        .exclusiveWith(HolderSet.direct(enchantmentGetter.getOrThrow(Enchantments.BINDING_CURSE)))
                        .build(SOULBOUND.location())
        );

        context.register(
                EXPERIENCE_SOULBOUND,
                Enchantment.enchantment(
                                Enchantment.definition(
                                        holderGetter.getOrThrow(EPRegistry.EXPERIENCE_SOULBOUND_ENCHANTABLE),
                                        holderGetter.getOrThrow(EPRegistry.EXPERIENCE_SOULBOUND_ENCHANTABLE),
                                        2,
                                        1,
                                        Enchantment.dynamicCost(1, 0),
                                        Enchantment.dynamicCost(100, 0),
                                        0,
                                        EquipmentSlotGroup.ARMOR
                                )
                        )
                        .withSpecialEffect(EPRegistry.EXPERIENCE_SOULBOUND.get(), false)
                        .exclusiveWith(HolderSet.direct(enchantmentGetter.getOrThrow(Enchantments.BINDING_CURSE)))
                        .build(EXPERIENCE_SOULBOUND.location())
        );
    }

    private static ResourceKey<Enchantment> createKey(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, Epitaphs.rl(name));
    }
}
