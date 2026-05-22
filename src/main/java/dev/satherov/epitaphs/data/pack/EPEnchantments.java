package dev.satherov.epitaphs.data.pack;

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
    
    public static final ResourceKey<Enchantment> SOULBOUND = EPEnchantments.createKey("soulbound");
    public static final ResourceKey<Enchantment> EXPERIENCE_SOULBOUND = EPEnchantments.createKey("experience_soulbound");
    
    public static void bootstrap(BootstrapContext<Enchantment> context) {
        HolderGetter<Item> items = context.lookup(Registries.ITEM);
        HolderGetter<Enchantment> enchantments = context.lookup(Registries.ENCHANTMENT);
        
        context.register(
                EPEnchantments.SOULBOUND,
                Enchantment.enchantment(
                                Enchantment.definition(
                                        items.getOrThrow(EPRegistry.SOULBOUND_ENCHANTABLE),
                                        items.getOrThrow(EPRegistry.SOULBOUND_ENCHANTABLE),
                                        1,
                                        1,
                                        Enchantment.constantCost(0),
                                        Enchantment.constantCost(0),
                                        1,
                                        EquipmentSlotGroup.ANY
                                )
                        )
                        .withSpecialEffect(EPRegistry.SOULBOUND.get(), false)
                        .exclusiveWith(HolderSet.direct(enchantments.getOrThrow(Enchantments.BINDING_CURSE)))
                        .build(EPEnchantments.SOULBOUND.identifier())
        );
        
        context.register(
                EPEnchantments.EXPERIENCE_SOULBOUND,
                Enchantment.enchantment(
                                Enchantment.definition(
                                        items.getOrThrow(EPRegistry.EXPERIENCE_SOULBOUND_ENCHANTABLE),
                                        items.getOrThrow(EPRegistry.EXPERIENCE_SOULBOUND_ENCHANTABLE),
                                        1,
                                        1,
                                        Enchantment.constantCost(0),
                                        Enchantment.constantCost(0),
                                        1,
                                        EquipmentSlotGroup.ARMOR
                                )
                        )
                        .withSpecialEffect(EPRegistry.EXPERIENCE_SOULBOUND.get(), false)
                        .exclusiveWith(HolderSet.direct(enchantments.getOrThrow(Enchantments.BINDING_CURSE)))
                        .build(EPEnchantments.EXPERIENCE_SOULBOUND.identifier())
        );
    }
    
    private static ResourceKey<Enchantment> createKey(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, Epitaphs.id(name));
    }
}
