package dev.satherov.epitaphs.compat;

import dev.satherov.epitaphs.Epitaphs;

import net.neoforged.fml.ModList;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;

import java.util.Optional;

public class AccessoriesHandler {
    
    public static boolean isLoaded() {
        return ModList.get().isLoaded("accessories");
    }
    
    public static void clearAll(ServerPlayer player) {
        Optional.ofNullable(AccessoriesCapability.get(player)).ifPresentOrElse(cap -> cap.getContainers().forEach((identifier, container) -> {
            final int size = container.getSize();
            ExpandedSimpleContainer stacks = container.getAccessories();
            for (int i = 0; i < size; i++) stacks.setItem(i, ItemStack.EMPTY);
            ExpandedSimpleContainer cosmetic = container.getCosmeticAccessories();
            for (int i = 0; i < size; i++) cosmetic.setItem(i, ItemStack.EMPTY);
        }), () -> Epitaphs.log.warn("No accessories capability found on player {} - {}", player.getGameProfile().getName(), player.getStringUUID()));
    }
}