package dev.satherov.epitaphs.compat;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import oshi.util.tuples.Pair;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CurioHandler {
    
    private static final String NEOFORGE_ATTACHMENTS = "neoforge:attachments";
    private static final String CURIOS_INVENTORY = "curios:inventory";
    private static final String CURIOS_DATA = "Curios";
    private static final String CURIO_DATA = "Curio";
    private static final String IDENTIFIER = "Identifier";
    private static final String STACKS_HANDLER = "StacksHandler";
    private static final String STACKS = "Stacks";
    private static final String COSMETICS = "Cosmetics";
    private static final String ITEMS = "Items";
    private static final String SIZE = "Size";
    
    public static int loadInventory(ServerPlayer player, CompoundTag root, boolean clear) {
        CompoundTag curioData = extractCurioData(root, CURIOS_DATA);
        if (curioData == null) return -1;
        
        ListTag data = curioData.getList(CURIOS_DATA, Tag.TAG_COMPOUND);
        if (data.isEmpty()) return -1;
        
        if (clear) clearCurio(player);
        
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> {
                    processCurioEntries(player, data, handler);
                    return 0;
                })
                .orElse(-1);
    }
    
    public static List<ItemStack> loadContents(MinecraftServer server, CompoundTag root) {
        List<ItemStack> contents = new ArrayList<>();
        
        CompoundTag curioData = extractCurioData(root, CURIOS_DATA);
        if (curioData == null) return contents;
        
        ListTag data = curioData.getList(CURIOS_DATA, Tag.TAG_COMPOUND);
        if (data.isEmpty()) return contents;
        
        ItemStackHandler loader = new ItemStackHandler();
        
        for (int i = 0; i < data.size(); i++) {
            CompoundTag stacksHandler = data.getCompound(i).getCompound(STACKS_HANDLER);
            
            loadFromHandler(server, stacksHandler, STACKS, loader, contents);
            loadFromHandler(server, stacksHandler, COSMETICS, loader, contents);
        }
        
        return contents;
    }
    
    public static void clearCurio(ServerPlayer player) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.getCurios().forEach((slot, curioHandler) -> {
                    clearSlots(curioHandler.getStacks());
                    clearSlots(curioHandler.getCosmeticStacks());
                })
        );
    }
    
    public static Pair<List<ItemStack>, List<ItemStack>> getCurio(ServerPlayer player) {
        List<ItemStack> contents = new ArrayList<>();
        List<ItemStack> cosmetics = new ArrayList<>();
        
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.getCurios().forEach((slot, curioHandler) -> {
                    addFromHandler(curioHandler.getStacks(), contents);
                    addFromHandler(curioHandler.getCosmeticStacks(), cosmetics);
                })
        );
        
        return new Pair<>(contents, cosmetics);
    }
    
    public static void setCurio(ServerPlayer player, List<ItemStack> stacks, List<ItemStack> cosmetics) {
        if (stacks.isEmpty() && cosmetics.isEmpty()) return;
        
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            Map<String, ICurioStacksHandler> curios = handler.getCurios();
            curios.forEach((slotId, curioHandler) -> {
                restoreSlot(curioHandler.getStacks(), stacks);
                restoreSlot(curioHandler.getCosmeticStacks(), cosmetics);
            });
        });
        
        stacks.forEach(stack -> {
            if (stack.isEmpty()) return;
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        });
        
        cosmetics.forEach(stack -> {
            if (stack.isEmpty()) return;
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        });
    }
    
    public static void removeCurio(ServerPlayer player, ItemStack stack, boolean cosmetic) {
        if (stack.isEmpty()) return;
        
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.getCurios().forEach((slot, curioHandler) -> {
                    if (cosmetic) removeFromSlots(curioHandler.getCosmeticStacks(), stack);
                    else removeFromSlots(curioHandler.getStacks(), stack);
                })
        );
    }
    
    public static boolean isEmpty(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.getCurios().values().stream()
                        .allMatch(curioHandler ->
                                isSlotsEmpty(curioHandler.getStacks()) &&
                                        isSlotsEmpty(curioHandler.getCosmeticStacks())
                        )
                )
                .orElse(true);
    }
    
    private static CompoundTag extractCurioData(CompoundTag root, String dataKey) {
        if (!root.contains(NEOFORGE_ATTACHMENTS)) return null;
        
        CompoundTag neo = root.getCompound(NEOFORGE_ATTACHMENTS);
        if (!neo.contains(CURIOS_INVENTORY)) return null;
        
        CompoundTag curios = neo.getCompound(CURIOS_INVENTORY);
        if (!curios.contains(dataKey)) return null;
        
        return curios;
    }
    
    private static void processCurioEntries(ServerPlayer player, ListTag data, ICuriosItemHandler handler) {
        for (int i = 0; i < data.size(); i++) {
            CompoundTag curioEntry = data.getCompound(i);
            
            if (!curioEntry.contains(IDENTIFIER) || !curioEntry.contains(STACKS_HANDLER)) {
                continue;
            }
            
            String identifier = curioEntry.getString(IDENTIFIER);
            CompoundTag stacksHandler = curioEntry.getCompound(STACKS_HANDLER);
            
            ICurioStacksHandler curioHandler = handler.getCurios().get(identifier);
            if (curioHandler == null) continue;
            
            processStacksData(player, stacksHandler, STACKS, curioHandler.getStacks());
            processStacksData(player, stacksHandler, COSMETICS, curioHandler.getCosmeticStacks());
        }
    }
    
    private static void processStacksData(ServerPlayer player, CompoundTag stacksHandler, String dataType, IDynamicStackHandler handler) {
        if (!stacksHandler.contains(dataType)) return;
        
        CompoundTag stacksData = stacksHandler.getCompound(dataType);
        if (!stacksData.contains(ITEMS) || stacksData.getList(ITEMS, Tag.TAG_COMPOUND).isEmpty()) {
            return;
        }
        
        ListTag items = stacksData.getList(ITEMS, Tag.TAG_COMPOUND);
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(player.registryAccess(), items.getCompound(i));
            if (stack.isEmpty()) continue;
            stacks.add(stack);
        }
        
        for (int i = 0; i < handler.getSlots(); i++) {
            for (int j = 0; j < stacks.size(); j++) {
                ItemStack stack = stacks.get(j);
                if (stack.isEmpty()) continue;
                
                if (handler.getStackInSlot(i).isEmpty() && handler.isItemValid(i, stack)) {
                    handler.setStackInSlot(i, stack);
                    stacks.set(j, ItemStack.EMPTY);
                }
            }
        }
        
        stacks.forEach(stack -> {
            if (stack.isEmpty()) return;
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        });
    }
    
    private static void loadFromHandler(MinecraftServer server, CompoundTag stacksHandler, String dataType, ItemStackHandler loader, List<ItemStack> contents) {
        CompoundTag stacksData = stacksHandler.getCompound(dataType);
        if (!stacksData.isEmpty()) {
            loader.deserializeNBT(server.registryAccess(), stacksData);
            addFromHandler(loader, contents);
        }
    }
    
    private static void addFromHandler(IItemHandler handler, List<ItemStack> contents) {
        for (int j = 0; j < handler.getSlots(); j++) {
            ItemStack stack = handler.getStackInSlot(j);
            if (stack.isEmpty()) continue;
            contents.add(stack);
        }
    }
    
    private static void clearSlots(IDynamicStackHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            handler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }
    
    private static void restoreSlot(IDynamicStackHandler handler, List<ItemStack> stacks) {
        for (int i = 0; i < handler.getSlots(); i++) {
            for (int j = 0; j < stacks.size(); j++) {
                ItemStack stack = stacks.get(j);
                if (stack.isEmpty()) continue;
                
                if (handler.getStackInSlot(i).isEmpty() && handler.isItemValid(i, stack)) {
                    handler.setStackInSlot(i, stack);
                    stacks.set(j, ItemStack.EMPTY);
                }
            }
        }
    }
    
    private static boolean removeFromSlots(IDynamicStackHandler handler, ItemStack stackToRemove) {
        for (int i = 0; i < handler.getSlots(); i++) {
            if (ItemStack.isSameItemSameComponents(handler.getStackInSlot(i), stackToRemove)) {
                handler.setStackInSlot(i, ItemStack.EMPTY);
                return true;
            }
        }
        return false;
    }
    
    private static boolean isSlotsEmpty(IDynamicStackHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            if (!handler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}