package dev.satherov.epitaphs.compat;

import net.neoforged.fml.ModList;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public class CuriosHandler {
    
    public static boolean isLoaded() {
        return ModList.get().isLoaded("curios");
    }
    
    public static void clearAll(ServerPlayer player) {
        CuriosApi.getCuriosInventory(player).ifPresent(curio -> curio.getCurios().forEach((identifier, handler) -> {
            IDynamicStackHandler stacks = handler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) stacks.setStackInSlot(i, ItemStack.EMPTY);
            IDynamicStackHandler cosmetic = handler.getCosmeticStacks();
            for (int i = 0; i < cosmetic.getSlots(); i++) cosmetic.setStackInSlot(i, ItemStack.EMPTY);
        }));
    }
}