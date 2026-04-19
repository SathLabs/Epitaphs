package dev.satherov.epitaphs.compat;

import lombok.experimental.UtilityClass;

import net.neoforged.fml.ModList;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

@UtilityClass
public class CuriosHandler {
    
    private static final String MOD_ID = "curios";
    
    public static boolean isLoaded() {
        return ModList.get().isLoaded(CuriosHandler.MOD_ID);
    }
    
    public static void clearAll(ServerPlayer player) {
        CuriosApi.getCuriosInventory(player).ifPresent(inventory ->
                inventory.getCurios().values().forEach(handler -> {
                    final IDynamicStackHandler items = handler.getStacks();
                    for (int slot = 0; slot < items.getSlots(); slot++) {
                        items.setStackInSlot(slot, ItemStack.EMPTY);
                    }
                    
                    final IDynamicStackHandler cosmetics = handler.getCosmeticStacks();
                    for (int slot = 0; slot < cosmetics.getSlots(); slot++) {
                        cosmetics.setStackInSlot(slot, ItemStack.EMPTY);
                    }
                })
        );
    }
}
