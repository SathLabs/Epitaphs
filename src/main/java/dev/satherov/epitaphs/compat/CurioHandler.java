package dev.satherov.epitaphs.compat;

import net.neoforged.neoforge.items.ItemStackHandler;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CurioHandler {

    public static int loadInventory(ServerPlayer player, CompoundTag root, boolean clear) {
        if (!root.contains("neoforge:attachments")) return -1;
        CompoundTag neo = root.getCompound("neoforge:attachments");
        if (!neo.contains("curios:inventory")) return -1;
        CompoundTag curios = neo.getCompound("curios:inventory");
        if (!curios.contains("Curios")) return -1;
        ListTag data = curios.getList("Curios", Tag.TAG_COMPOUND);

        if (data.isEmpty()) return -1;

        if (clear) clearCurio(player);

        return CuriosApi.getCuriosInventory(player).map(handler -> {
            for (int i = 0; i < data.size(); i++) {
                CompoundTag curioEntry = data.getCompound(i);
                if (!curioEntry.contains("Identifier") || !curioEntry.contains("StacksHandler")) {
                    continue;
                }

                String identifier = curioEntry.getString("Identifier");
                CompoundTag stacksHandler = curioEntry.getCompound("StacksHandler");

                ICurioStacksHandler curioHandler = handler.getCurios().get(identifier);
                if (curioHandler == null) {
                    continue;
                }


                if (stacksHandler.contains("Stacks")) {
                    CompoundTag stacksData = stacksHandler.getCompound("Stacks");
                    int size = stacksData.getInt("Size");

                    if (stacksData.contains("Items") && !stacksData.getList("Items", Tag.TAG_COMPOUND).isEmpty()) {
                        ListTag items = stacksData.getList("Items", Tag.TAG_COMPOUND);
                        IDynamicStackHandler stacks = curioHandler.getStacks();

                        for (int j = 0; j < Math.min(size, items.size()); j++) {
                            if (j < stacks.getSlots()) {
                                CompoundTag itemTag = items.getCompound(j);
                                ItemStack stack = ItemStack.parseOptional(player.registryAccess(), itemTag);
                                if (stacks.getStackInSlot(j).isEmpty()) {
                                    stacks.setStackInSlot(j, stack);
                                    continue;
                                }
                                if (!player.getInventory().add(stack)) {
                                    player.drop(stack, false);
                                }
                            }
                        }
                    }
                }

                if (stacksHandler.contains("Cosmetics")) {
                    CompoundTag cosmeticsData = stacksHandler.getCompound("Cosmetics");
                    int size = cosmeticsData.getInt("Size");

                    if (cosmeticsData.contains("Items") && !cosmeticsData.getList("Items", Tag.TAG_COMPOUND).isEmpty()) {
                        ListTag items = cosmeticsData.getList("Items", Tag.TAG_COMPOUND);
                        IDynamicStackHandler cosmetics = curioHandler.getCosmeticStacks();

                        for (int j = 0; j < Math.min(size, items.size()); j++) {
                            if (j < cosmetics.getSlots()) {
                                CompoundTag itemTag = items.getCompound(j);
                                ItemStack stack = ItemStack.parseOptional(player.registryAccess(), itemTag);
                                if (cosmetics.getStackInSlot(j).isEmpty()) {
                                    cosmetics.setStackInSlot(j, stack);
                                    continue;
                                }
                                if (!player.getInventory().add(stack)) {
                                    player.drop(stack, false);
                                }
                            }
                        }
                    }
                }
            }

            return 0;
        }).orElse(-1);
    }

    public static List<ItemStack> loadContents(MinecraftServer server, CompoundTag root) {
        List<ItemStack> contents = new ArrayList<>();

        if (!root.contains("neoforge:attachments")) return contents;
        CompoundTag neo = root.getCompound("neoforge:attachments");
        if (!neo.contains("curios:inventory")) return contents;
        CompoundTag curio = neo.getCompound("curios:inventory");
        if (!curio.contains("Curio")) return contents;
        ListTag data = curio.getList("Curio", Tag.TAG_COMPOUND);

        if (data.isEmpty()) return contents;

        ItemStackHandler loaded = new ItemStackHandler();

        for (int i = 0; i < data.size(); i++) {
            CompoundTag tag = data.getCompound(i).getCompound("StacksHandler");

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
            content.add(loadedStack);
        }
        return content;
    }

    public static void clearCurio(ServerPlayer player) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            handler.getCurios().forEach((slot, stack) -> {
                for (int i = 0; i < stack.getStacks().getSlots(); i++) {
                    stack.getStacks().setStackInSlot(i, ItemStack.EMPTY);
                }
                for (int i = 0; i < stack.getCosmeticStacks().getSlots(); i++) {
                    stack.getCosmeticStacks().setStackInSlot(i, ItemStack.EMPTY);
                }
            });
        });
    }

    public static NonNullList<ItemStack> getCurio(ServerPlayer player) {
        NonNullList<ItemStack> contents = NonNullList.create();

        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.getCurios().forEach((slot, curioHandler) -> {
                    for (int i = 0; i < curioHandler.getStacks().getSlots(); i++) {
                        contents.add(curioHandler.getStacks().getStackInSlot(i));
                    }
                    for (int i = 0; i < curioHandler.getCosmeticStacks().getSlots(); i++) {
                        contents.add(curioHandler.getCosmeticStacks().getStackInSlot(i));
                    }
                })
        );

        return contents;
    }

    public static List<ItemStack> setCurio(ServerPlayer player, NonNullList<ItemStack> stacks) {
        List<ItemStack> overflow = new ArrayList<>();
        if (stacks.isEmpty()) return overflow;

        AtomicInteger stackIndex = new AtomicInteger();

        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.getCurios().forEach((slotId, curioHandler) -> {
                    restoreCurioSlots(curioHandler.getStacks(), stacks, stackIndex, overflow);
                    restoreCurioSlots(curioHandler.getCosmeticStacks(), stacks, stackIndex, overflow);
                })
        );

        return overflow;
    }

    private static void restoreCurioSlots(IDynamicStackHandler handler, NonNullList<ItemStack> stacks, AtomicInteger stackIndex, List<ItemStack> overflow) {
        for (int i = 0; i < handler.getSlots(); i++) {
            if (stackIndex.get() >= stacks.size()) break;

            ItemStack stackToRestore = stacks.get(stackIndex.getAndIncrement());
            if (stackToRestore.isEmpty()) continue;

            if (handler.getStackInSlot(i).isEmpty()) {
                handler.setStackInSlot(i, stackToRestore);
            } else {
                overflow.add(stackToRestore);
            }
        }
    }

    public static void removeCurio(ServerPlayer player, ItemStack stack) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            handler.getCurios().forEach((slot, curioHandler) -> {
                for (int i = 0; i < curioHandler.getStacks().getSlots(); i++) {
                    if (ItemStack.isSameItem(curioHandler.getStacks().getStackInSlot(i), stack)) {
                        curioHandler.getStacks().setStackInSlot(i, ItemStack.EMPTY);
                        return;
                    }
                }
                for (int i = 0; i < curioHandler.getCosmeticStacks().getSlots(); i++) {
                    if (ItemStack.isSameItem(curioHandler.getCosmeticStacks().getStackInSlot(i), stack)) {
                        curioHandler.getCosmeticStacks().setStackInSlot(i, ItemStack.EMPTY);
                        return;
                    }
                }
            });
        });
    }

    public static boolean isEmpty(ServerPlayer player) {
        class FoundItemException extends RuntimeException {
            @Override
            public synchronized Throwable fillInStackTrace() {
                return this;
            }
        }

        try {
            CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                    handler.getCurios().forEach((slot, curioHandler) -> {
                        for (int i = 0; i < curioHandler.getStacks().getSlots(); i++) {
                            if (!curioHandler.getStacks().getStackInSlot(i).isEmpty()) {
                                throw new FoundItemException();
                            }
                        }
                        for (int i = 0; i < curioHandler.getCosmeticStacks().getSlots(); i++) {
                            if (!curioHandler.getCosmeticStacks().getStackInSlot(i).isEmpty()) {
                                throw new FoundItemException();
                            }
                        }
                    })
            );
            return true;
        } catch (FoundItemException e) {
            return false;
        }
    }

}