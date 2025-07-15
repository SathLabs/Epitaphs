package dev.satherov.epitaphs.compat;

import net.neoforged.fml.ModList;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedList;
import java.util.List;

public class EPCompatHandler {

    private static boolean isCurioLoaded() {
        return ModList.get().isLoaded("curios");
    }

    public static ListTag saveCurioInventory(ServerPlayer player, boolean clear) {
        if (isCurioLoaded()) {
            return EPCurioHandler.saveInventory(player, clear);
        }
        return new ListTag();
    }

    public static void loadCurioInventory(ServerPlayer player, ListTag data, boolean clear) {
        if (isCurioLoaded()) {
            EPCurioHandler.loadInventory(player, data, clear);
        }
    }

    public static List<ItemStack> loadCurioContents(ServerLevel level, CompoundTag root) {
        if (isCurioLoaded()) {
            return EPCurioHandler.loadContents(level, root);
        }
        return new LinkedList<>();
    }
}
