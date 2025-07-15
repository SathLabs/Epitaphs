package dev.satherov.epitaphs.compat;

import net.neoforged.neoforge.items.ItemStackHandler;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;

public class EPCurioHandler {

    public static ListTag saveInventory(ServerPlayer player, boolean clear) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.saveInventory(clear))
                .orElse(new ListTag());
    }

    public static void loadInventory(ServerPlayer player, ListTag data, boolean clear) {
        if (clear) clearCurio(player);
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            handler.loadInventory(data);
        });
    }

    public static List<ItemStack> loadContents(ServerLevel level, CompoundTag root) {
        ListTag data = root.getList("curio", Tag.TAG_COMPOUND);
        List<ItemStack> contents = new ArrayList<>();
        ItemStackHandler loaded = new ItemStackHandler();

        for (int i = 0; i < data.size(); i++) {
            CompoundTag tag = data.getCompound(i);

            CompoundTag stacksData = tag.getCompound("Stacks");
            if (!stacksData.isEmpty()) {
                loaded.deserializeNBT(level.registryAccess(), stacksData);
                contents.addAll(loadStacks(loaded));
            }

            stacksData = tag.getCompound("Cosmetics");
            if (!stacksData.isEmpty()) {
                loaded.deserializeNBT(level.registryAccess(), stacksData);
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
                stack.getCosmeticStacks().setStackInSlot(1, ItemStack.EMPTY);
            });
        });
    }
}