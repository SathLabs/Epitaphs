package dev.satherov.epitaphs.compat;

import net.neoforged.fml.ModList;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedList;
import java.util.List;

public class CompatHandler {

    private static boolean isCurioLoaded() {
        return ModList.get().isLoaded("curios");
    }

    public static ListTag saveCurioInventory(ServerPlayer player, boolean clear) {
        if (isCurioLoaded()) return CurioHandler.saveInventory(player, clear);
        return new ListTag();
    }

    public static int loadCurioInventory(ServerPlayer player, CompoundTag data, boolean clear) {
        if (isCurioLoaded()) return CurioHandler.loadInventory(player, data, clear);
        return 0;
    }

    public static List<ItemStack> loadCurioContents(MinecraftServer server, CompoundTag root) {
        if (isCurioLoaded()) return CurioHandler.loadContents(server, root);
        return new LinkedList<>();
    }
}
