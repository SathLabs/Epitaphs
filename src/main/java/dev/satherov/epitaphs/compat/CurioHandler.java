package dev.satherov.epitaphs.compat;

import dev.satherov.epitaphs.util.EPTagUtil;

import net.neoforged.neoforge.items.ItemStackHandler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;

public class CurioHandler {

    public static ListTag saveInventory(ServerPlayer player, boolean clear) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.saveInventory(clear))
                .orElse(new ListTag());
    }

    public static int loadInventory(ServerPlayer player, CompoundTag root, boolean clear) {
        ListTag data = EPTagUtil
                .getSafe(root, "neoforge:attachments")
                .getSafe("curios:inventory")
                .getFinal("Curios", Tag.TAG_COMPOUND);
        if (data.isEmpty()) return -1;

        if (clear) clearCurio(player);
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            handler.loadInventory(data);
        });
        return 0;
    }

    public static List<ItemStack> loadContents(MinecraftServer server, CompoundTag root) {
        List<ItemStack> contents = new ArrayList<>();

        ListTag data = EPTagUtil
                .getSafe(root, "neoforge:attachments")
                .getSafe("curios:inventory")
                .getFinal("Curios", Tag.TAG_COMPOUND);
        if (data.isEmpty()) return contents;

        ItemStackHandler loaded = new ItemStackHandler();

        for (int i = 0; i < data.size(); i++) {
            CompoundTag tag = data.getCompound(i);

            CompoundTag stacksData = tag.getCompound("Stacks");
            if (!stacksData.isEmpty()) {
                loaded.deserializeNBT(server.registryAccess(), stacksData);
                contents.addAll(loadStacks(loaded));
            }

            stacksData = tag.getCompound("Cosmetics");
            if (!stacksData.isEmpty()) {
                loaded.deserializeNBT(server.registryAccess(), stacksData);
                contents.addAll(loadStacks(loaded));
            }
        }

        return contents;
    }

    private static List<ItemStack> loadStacks(ItemStackHandler loaded) {
        List<ItemStack> content = new ArrayList<>();
        for (int j = 0; j < loaded.getSlots(); j++) {
            ItemStack loadedStack = loaded.getStackInSlot(j);
            content.set(j, loadedStack);
        }
        return content;
    }

    public static void clearCurio(ServerPlayer player) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            handler.getCurios().forEach((slot, stack) -> {
                stack.getStacks().setStackInSlot(0, ItemStack.EMPTY);
                stack.getCosmeticStacks().setStackInSlot(0, ItemStack.EMPTY);
            });
        });
    }
}