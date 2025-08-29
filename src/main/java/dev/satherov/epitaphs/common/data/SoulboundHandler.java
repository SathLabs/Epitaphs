package dev.satherov.epitaphs.common.data;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.common.component.EPSoulboundAttachment;
import dev.satherov.epitaphs.compat.CompatHandler;
import dev.satherov.epitaphs.core.EPRegistry;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.List;

public class SoulboundHandler {

    public static void handleSoulbound(ServerPlayer player) {
        EPSoulboundAttachment attachment = player.getData(EPRegistry.SOULBOUND_DATA);

        NonNullList<ItemStack> soulboundItems = extractSoulboundItems(player.getInventory().items);
        NonNullList<ItemStack> soulboundArmor = extractSoulboundItems(player.getInventory().armor);
        NonNullList<ItemStack> soulboundOffhand = extractSoulboundItems(player.getInventory().offhand);
        NonNullList<ItemStack> soulboundCurios = extractSoulboundItems(CompatHandler.getCurio(player));
        
        attachment.setItems(player, soulboundItems);
        attachment.setArmor(player, soulboundArmor);
        attachment.setOffhand(player, soulboundOffhand);
        attachment.setCurio(player, soulboundCurios);

        for (ItemStack stack : soulboundCurios) {
            if (!stack.isEmpty()) {
                CompatHandler.removeCurio(player, stack);
            }
        }

        player.setData(EPRegistry.SOULBOUND_DATA, attachment);
    }

    public static void restoreSoulbound(ServerPlayer player) {
        EPSoulboundAttachment attachment = player.getData(EPRegistry.SOULBOUND_DATA);

        restoreItemsToSlots(player, player.getInventory().items, attachment.getItems());
        restoreItemsToSlots(player, player.getInventory().armor, attachment.getArmor());
        restoreItemsToSlots(player, player.getInventory().offhand, attachment.getOffhand());

        List<ItemStack> overFlow = CompatHandler.setCurio(player, attachment.getCurio());
        if (!overFlow.isEmpty()) {
            for (ItemStack stack : overFlow) {
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }
        }

        int experience = attachment.getExperience();
        if (experience > 0) {
            player.giveExperiencePoints(experience);
        }

        player.getData(EPRegistry.SOULBOUND_DATA).clear();
    }

    public static int handleXpSoulbound(ServerPlayer player) {
        EPSoulboundAttachment attachment = player.getData(EPRegistry.SOULBOUND_DATA);
        NonNullList<ItemStack> armor = attachment.getArmor();

        int xpSoulboundCount = 0;
        for (ItemStack stack : armor) {
            if (!stack.isEmpty() && hasXpSoulbound(stack)) {
                xpSoulboundCount++;
            }
        }

        return xpSoulboundCount;
    }

    private static NonNullList<ItemStack> extractSoulboundItems(NonNullList<ItemStack> originalList) {
        NonNullList<ItemStack> soulboundItems = NonNullList.withSize(originalList.size(), ItemStack.EMPTY);

        for (int i = 0; i < originalList.size(); i++) {
            ItemStack stack = originalList.get(i);
            if (hasSoulbound(stack)) {
                soulboundItems.set(i, stack.copy());
                originalList.set(i, ItemStack.EMPTY);
            }
        }

        return soulboundItems;
    }

    private static void restoreItemsToSlots(ServerPlayer player, NonNullList<ItemStack> targetList, NonNullList<ItemStack> soulboundList) {
        for (int i = 0; i < Math.min(targetList.size(), soulboundList.size()); i++) {
            ItemStack stack = soulboundList.get(i);
            if (stack.isEmpty()) continue;

            if (targetList.get(i).isEmpty()) {
                targetList.set(i, stack);
            } else if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    private static boolean hasSoulbound(ItemStack stack) {
        return EnchantmentHelper.has(stack, EPRegistry.SOULBOUND.get()) ||
                EnchantmentHelper.has(stack, EPRegistry.EXPERIENCE_SOULBOUND.get());
    }

    private static boolean hasXpSoulbound(ItemStack stack) {
        return EnchantmentHelper.has(stack, EPRegistry.EXPERIENCE_SOULBOUND.get());
    }
}