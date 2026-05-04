package dev.satherov.epitaphs.common.container;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.common.data.SoulboundHandler;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({ "LoggingSimilarMessage", "DuplicatedCode" })
public record CuriosContainer(Map<String, StackHandler> entries) implements SaveContainer<CuriosContainer> {
    
    public static final Codec<CuriosContainer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, StackHandler.CODEC).fieldOf("entries").forGetter(CuriosContainer::entries)
    ).apply(instance, CuriosContainer::new));
    
    public static CuriosContainer empty() {
        return new CuriosContainer(new HashMap<>());
    }
    
    // ==================== ONLINE ====================
    
    public static CuriosContainer create(ServerPlayer player) {
        final Map<String, StackHandler> entries = new HashMap<>();
        CuriosApi.getCuriosInventory(player).ifPresentOrElse(inventory -> {
            inventory.getCurios().forEach((key, value) -> {
                final IDynamicStackHandler itemStacks = value.getStacks();
                final NonNullList<ItemStack> items = NonNullList.withSize(itemStacks.getSlots(), ItemStack.EMPTY);
                for (int slot = 0; slot < itemStacks.getSlots(); slot++) {
                    items.set(slot, itemStacks.getStackInSlot(slot).copy());
                }
                
                final IDynamicStackHandler cosmeticStacks = value.getCosmeticStacks();
                final NonNullList<ItemStack> cosmetics = NonNullList.withSize(cosmeticStacks.getSlots(), ItemStack.EMPTY);
                for (int slot = 0; slot < cosmeticStacks.getSlots(); slot++) {
                    cosmetics.set(slot, cosmeticStacks.getStackInSlot(slot).copy());
                }
                
                entries.put(key, new StackHandler(items, cosmetics));
            });
        }, () -> Epitaphs.log.warn("No curios capability found on player {} - {}", player.getGameProfile().getName(), player.getStringUUID()));
        return new CuriosContainer(entries);
    }
    
    @Override
    public void write(ServerPlayer player) {
        CuriosApi.getCuriosInventory(player).ifPresentOrElse(inventory -> {
            inventory.getCurios().forEach((key, value) -> {
                final StackHandler handler = this.entries.get(key);
                if (handler == null) return;
                if (handler.isEmpty()) return;
                
                final IDynamicStackHandler itemStacks = value.getStacks();
                final NonNullList<ItemStack> items = handler.items();
                for (int slot = 0; slot < items.size(); slot++) {
                    ItemStack stack = items.get(slot).copyAndClear();
                    if (slot < itemStacks.getSlots()) {
                        itemStacks.setStackInSlot(slot, stack);
                        continue;
                    }
                    
                    for (int i = 0; i < itemStacks.getSlots(); i++) {
                        if (itemStacks.getStackInSlot(i).isEmpty()) {
                            itemStacks.setStackInSlot(i, stack);
                            break;
                        }
                    }
                }
                
                final IDynamicStackHandler cosmeticStacks = value.getCosmeticStacks();
                final NonNullList<ItemStack> cosmetics = handler.cosmetics();
                for (int slot = 0; slot < cosmetics.size(); slot++) {
                    ItemStack stack = cosmetics.get(slot).copyAndClear();
                    if (slot < cosmeticStacks.getSlots()) {
                        cosmeticStacks.setStackInSlot(slot, stack);
                        continue;
                    }
                    
                    for (int i = 0; i < cosmeticStacks.getSlots(); i++) {
                        if (cosmeticStacks.getStackInSlot(i).isEmpty()) {
                            cosmeticStacks.setStackInSlot(i, stack);
                            break;
                        }
                    }
                }
            });
        }, () -> Epitaphs.log.warn("No curios capability found on player {} - {}", player.getGameProfile().getName(), player.getStringUUID()));
    }
    
    public static CuriosContainer createSoulbound(ServerPlayer player) {
        final Map<String, StackHandler> entries = new HashMap<>();
        CuriosApi.getCuriosInventory(player).ifPresentOrElse(inventory -> {
            inventory.getCurios().forEach((key, value) -> {
                final IDynamicStackHandler itemStacks = value.getStacks();
                final NonNullList<ItemStack> items = NonNullList.withSize(itemStacks.getSlots(), ItemStack.EMPTY);
                for (int slot = 0; slot < itemStacks.getSlots(); slot++) {
                    ItemStack stack = itemStacks.getStackInSlot(slot);
                    if (SoulboundHandler.isSoulbound(stack)) items.set(slot, stack.copyAndClear());
                }
                
                final IDynamicStackHandler cosmeticStacks = value.getCosmeticStacks();
                final NonNullList<ItemStack> cosmetics = NonNullList.withSize(cosmeticStacks.getSlots(), ItemStack.EMPTY);
                for (int slot = 0; slot < cosmeticStacks.getSlots(); slot++) {
                    ItemStack stack = cosmeticStacks.getStackInSlot(slot);
                    if (SoulboundHandler.isSoulbound(stack)) cosmeticStacks.setStackInSlot(slot, stack.copyAndClear());
                }
                
                entries.put(key, new StackHandler(items, cosmetics));
            });
        }, () -> Epitaphs.log.warn("No curios capability found on player {} - {}", player.getGameProfile().getName(), player.getStringUUID()));
        return new CuriosContainer(entries);
    }
    
    // ==================== OFFLINE ====================
    
    public static CuriosContainer create(HolderLookup.Provider provider, CompoundTag data) {
        final Map<String, StackHandler> entries = new HashMap<>();
        final CompoundTag attachments = data.getCompound("neoforge:attachments");
        final CompoundTag curiosInventory = attachments.getCompound("curios:inventory");
        final ListTag curios = curiosInventory.getList("Curios", Tag.TAG_COMPOUND);
        for (int i = 0; i < curios.size(); i++) {
            final CompoundTag entry = curios.getCompound(i);
            final String identifier = entry.getString("Identifier");
            final CompoundTag handler = entry.getCompound("StacksHandler");
            
            final NonNullList<ItemStack> items = StackHandler.createList(provider, handler.getCompound("Stacks"));
            final NonNullList<ItemStack> cosmetics = StackHandler.createList(provider, handler.getCompound("Cosmetics"));
            entries.put(identifier, new StackHandler(items, cosmetics));
        }
        return new CuriosContainer(entries);
    }
    
    @Override
    public void write(HolderLookup.Provider provider, CompoundTag data) {
        final CompoundTag attachments = data.getCompound("neoforge:attachments");
        final CompoundTag curiosInventory = attachments.getCompound("curios:inventory");
        final ListTag curios = curiosInventory.getList("Curios", Tag.TAG_COMPOUND);
        for (int i = 0; i < curios.size(); i++) {
            final CompoundTag entry = curios.getCompound(i);
            final String identifier = entry.getString("Identifier");
            final CompoundTag stacksHandler = entry.getCompound("StacksHandler");
            final StackHandler handler = this.entries.get(identifier);
            if (handler == null) continue;
            if (handler.isEmpty()) continue;
            
            CompoundTag stacks = StackHandler.writeList(provider, handler.items());
            stacksHandler.put("Stacks", stacks);
            
            CompoundTag cosmetics = StackHandler.writeList(provider, handler.cosmetics());
            stacksHandler.put("Cosmetics", cosmetics);
            entry.put("StacksHandler", stacksHandler);
        }
    }
    
    // ==================== OTHER ====================
    
    @Override
    public List<ItemStack> merge(CuriosContainer other) {
        final List<ItemStack> overflow = new ArrayList<>();
        for (Map.Entry<String, StackHandler> entry : other.entries.entrySet()) {
            final String identifier = entry.getKey();
            final StackHandler handler = entry.getValue();
            final StackHandler existing = this.entries.get(identifier);
            if (existing == null) this.entries.put(identifier, handler);
            else overflow.addAll(existing.merge(handler));
        }
        return overflow;
    }
    
    @Override
    public List<ItemStack> gather() {
        final List<ItemStack> result = new ArrayList<>();
        this.entries.forEach((key, stackHandler) -> result.addAll(stackHandler.gather()));
        return result;
    }
    
    @Override
    public boolean isEmpty() {
        return this.entries.isEmpty() || this.entries.values().stream().allMatch(StackHandler::isEmpty);
    }
    
    private record StackHandler(NonNullList<ItemStack> items, NonNullList<ItemStack> cosmetics) {
        
        private static final Codec<StackHandler> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                NonNullList.codecOf(ItemStack.OPTIONAL_CODEC).fieldOf("items").forGetter(StackHandler::items),
                NonNullList.codecOf(ItemStack.OPTIONAL_CODEC).fieldOf("cosmetics").forGetter(StackHandler::cosmetics)
        ).apply(instance, StackHandler::new));
        
        private static NonNullList<ItemStack> createList(HolderLookup.Provider provider, CompoundTag data) {
            final int size = data.getInt("Size");
            final ListTag stacks = data.getList("Items", Tag.TAG_COMPOUND);
            final NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
            for (int i = 0; i < stacks.size(); i++) {
                CompoundTag tag = stacks.getCompound(i);
                int slot = tag.getInt("Slot");
                if (slot < 0 || slot >= size) continue;
                ItemStack.parse(provider, tag).ifPresent(stack -> items.set(slot, stack));
            }
            return items;
        }
        
        private static CompoundTag writeList(HolderLookup.Provider provider, NonNullList<ItemStack> items) {
            ListTag list = new ListTag();
            for (int i = 0; i < items.size(); i++) {
                ItemStack stack = items.get(i).copyAndClear();
                if (stack.isEmpty()) continue;
                CompoundTag tag = new CompoundTag();
                tag.putInt("Slot", i);
                list.add(stack.save(provider, tag));
            }
            CompoundTag data = new CompoundTag();
            data.put("Items", list);
            data.putInt("Size", items.size());
            return data;
        }
        
        private List<ItemStack> merge(StackHandler other) {
            final List<ItemStack> overflow = new ArrayList<>();
            for (int slot = 0; slot < other.items.size(); slot++) {
                final ItemStack stack = other.items.get(slot).copyAndClear();
                if (stack.isEmpty()) continue;
                if (slot >= this.items.size()) {
                    overflow.add(stack);
                    continue;
                }
                final ItemStack existing = this.items.get(slot);
                if (existing.isEmpty()) this.items.set(slot, stack);
                else overflow.add(stack);
            }
            for (int slot = 0; slot < other.cosmetics.size(); slot++) {
                final ItemStack stack = other.cosmetics.get(slot).copyAndClear();
                if (stack.isEmpty()) continue;
                if (slot >= this.cosmetics.size()) {
                    overflow.add(stack);
                    continue;
                }
                final ItemStack existing = this.cosmetics.get(slot);
                if (existing.isEmpty()) this.cosmetics.set(slot, stack);
                else overflow.add(stack);
            }
            return overflow;
        }
        
        private List<ItemStack> gather() {
            final List<ItemStack> result = new ArrayList<>();
            this.items.forEach(stack -> result.add(stack.copyAndClear()));
            this.cosmetics.forEach(stack -> result.add(stack.copyAndClear()));
            return result;
        }
        
        private boolean isEmpty() {
            return (this.items.isEmpty() || this.items.stream().allMatch(ItemStack::isEmpty)) &&
                    (this.cosmetics.isEmpty() || this.cosmetics.stream().allMatch(ItemStack::isEmpty));
        }
    }
}
