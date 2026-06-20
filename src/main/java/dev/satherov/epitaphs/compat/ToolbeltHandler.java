package dev.satherov.epitaphs.compat;

import lombok.experimental.UtilityClass;

import net.neoforged.fml.ModList;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import dev.gigaherz.toolbelt.slot.BeltAttachment;

@UtilityClass
public class ToolbeltHandler {
    
    private static final String MOD_ID = "toolbelt";
    
    public static boolean isLoaded() {
        return ModList.get().isLoaded(ToolbeltHandler.MOD_ID);
    }
    
    public static void clear(ServerPlayer player) {
        BeltAttachment.get(player).setContents(ItemStack.EMPTY);
    }
}
