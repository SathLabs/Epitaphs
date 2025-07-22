package dev.satherov.epitaphs.compat;

import net.neoforged.fml.ModList;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CompatHandler {

    // Curio

    private static boolean isCurioLoaded() {
        return ModList.get().isLoaded("curios");
    }

    public static int loadCurioInventory(ServerPlayer player, CompoundTag data, boolean clear) {
        if (isCurioLoaded()) return CurioHandler.loadInventory(player, data, clear);
        return 0;
    }

    public static List<ItemStack> loadCurioContents(MinecraftServer server, CompoundTag root) {
        if (isCurioLoaded()) return CurioHandler.loadContents(server, root);
        return new ArrayList<>();
    }

    public static void clearCurio(ServerPlayer player) {
        if (isCurioLoaded()) CurioHandler.clearCurio(player);
    }

    public static NonNullList<ItemStack> getCurio(ServerPlayer player) {
        if (isCurioLoaded()) return CurioHandler.getCurio(player);
        return NonNullList.create();
    }

    public static List<ItemStack> setCurio(ServerPlayer player, NonNullList<ItemStack> stacks) {
        if (isCurioLoaded()) return CurioHandler.setCurio(player, stacks);
        return new ArrayList<>();
    }

    public static void removeCurio(ServerPlayer player, ItemStack stack) {
        if (isCurioLoaded()) CurioHandler.removeCurio(player, stack);
    }

    public static boolean isCurioEmpty(ServerPlayer player) {
        if (isCurioLoaded()) return CurioHandler.isEmpty(player);
        return true;
    }

    // FTB Chunks

    private static boolean isFTBChunksLoaded() {
        return ModList.get().isLoaded("ftbchunks");
    }

    public static boolean preventInteraction(ServerPlayer player, BlockPos pos) {
        if (isFTBChunksLoaded()) return FTBChunksHandler.preventInteractions(player, pos);
        return false;
    }
}
