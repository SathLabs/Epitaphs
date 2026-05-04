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

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({ "LoggingSimilarMessage", "DuplicatedCode" })
public record AccessoriesContainer(Map<String, StackHandler> entries) implements SaveContainer<AccessoriesContainer> {
    
    public static final Codec<AccessoriesContainer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, StackHandler.CODEC).fieldOf("entries").forGetter(AccessoriesContainer::entries)
    ).apply(instance, AccessoriesContainer::new));
    
    public static AccessoriesContainer empty() {
        return new AccessoriesContainer(new HashMap<>());
    }
    
    // ==================== ONLINE ====================
    
    public static AccessoriesContainer create(ServerPlayer player) {
        final GameProfile profile = player.getGameProfile();
        
        final Map<String, StackHandler> entries = new HashMap<>();
        Optional.ofNullable(AccessoriesCapability.get(player)).ifPresentOrElse(cap -> {
            cap.getContainers().forEach((key, value) -> {
                final ExpandedSimpleContainer itemStacks = value.getAccessories();
                final NonNullList<ItemStack> items = NonNullList.withSize(itemStacks.getContainerSize(), ItemStack.EMPTY);
                for (int slot = 0; slot < itemStacks.getContainerSize(); slot++) {
                    items.set(slot, itemStacks.getItem(slot).copy());
                }
                
                final ExpandedSimpleContainer cosmeticStacks = value.getCosmeticAccessories();
                final NonNullList<ItemStack> cosmetics = NonNullList.withSize(cosmeticStacks.getContainerSize(), ItemStack.EMPTY);
                for (int slot = 0; slot < cosmeticStacks.getContainerSize(); slot++) {
                    cosmetics.set(slot, cosmeticStacks.getItem(slot).copy());
                }
                
                entries.put(key, new StackHandler(items, cosmetics));
            });
        }, () -> Epitaphs.log.warn("No accessories capability found on player {} - {}", profile.getName(), profile.getId()));
        return new AccessoriesContainer(entries);
    }
    
    @Override
    public void write(ServerPlayer player) {
        final GameProfile profile = player.getGameProfile();
        
        Optional.ofNullable(AccessoriesCapability.get(player)).ifPresentOrElse(cap -> {
            cap.getContainers().forEach((key, value) -> {
                final StackHandler handler = this.entries.get(key);
                if (handler == null) return;
                if (handler.isEmpty()) return;
                
                final int size = value.getSize();
                final ExpandedSimpleContainer itemStacks = value.getAccessories();
                final NonNullList<ItemStack> items = handler.items();
                for (int slot = 0; slot < items.size(); slot++) {
                    ItemStack stack = items.get(slot).copyAndClear();
                    if (slot < size) {
                        itemStacks.setItem(slot, stack);
                        continue;
                    }
                    
                    for (int i = 0; i < size; i++) {
                        if (itemStacks.getItem(i).isEmpty())
                            itemStacks.setItem(i, stack);
                    }
                }
                
                final ExpandedSimpleContainer cosmeticStacks = value.getCosmeticAccessories();
                final NonNullList<ItemStack> cosmetics = handler.cosmetics();
                for (int slot = 0; slot < cosmetics.size(); slot++) {
                    ItemStack stack = cosmetics.get(slot).copyAndClear();
                    if (slot < size) {
                        cosmeticStacks.setItem(slot, stack);
                        continue;
                    }
                    
                    for (int i = 0; i < size; i++) {
                        if (cosmeticStacks.getItem(i).isEmpty())
                            cosmeticStacks.setItem(i, stack);
                    }
                }
            });
        }, () -> Epitaphs.log.warn("No accessories capability found on player {} - {}", profile.getName(), profile.getId()));
    }
    
    public static AccessoriesContainer createSoulbound(ServerPlayer player) {
        final GameProfile profile = player.getGameProfile();
        
        final Map<String, StackHandler> entries = new HashMap<>();
        Optional.ofNullable(AccessoriesCapability.get(player)).ifPresentOrElse(cap -> {
            cap.getContainers().forEach((key, value) -> {
                final ExpandedSimpleContainer itemStacks = value.getAccessories();
                final NonNullList<ItemStack> items = NonNullList.withSize(itemStacks.getContainerSize(), ItemStack.EMPTY);
                for (int slot = 0; slot < itemStacks.getContainerSize(); slot++) {
                    ItemStack stack = itemStacks.getItem(slot);
                    if (SoulboundHandler.isSoulbound(stack)) items.set(slot, stack.copyAndClear());
                }
                
                final ExpandedSimpleContainer cosmeticStacks = value.getCosmeticAccessories();
                final NonNullList<ItemStack> cosmetics = NonNullList.withSize(cosmeticStacks.getContainerSize(), ItemStack.EMPTY);
                for (int slot = 0; slot < cosmeticStacks.getContainerSize(); slot++) {
                    ItemStack stack = cosmeticStacks.getItem(slot);
                    if (SoulboundHandler.isSoulbound(stack)) cosmeticStacks.setItem(slot, stack.copyAndClear());
                }
                
                entries.put(key, new StackHandler(items, cosmetics));
            });
        }, () -> Epitaphs.log.warn("No accessories capability found on player {} - {}", profile.getName(), profile.getId()));
        return new AccessoriesContainer(entries);
    }
    
    // ==================== OFFLINE ====================
    
    public static AccessoriesContainer create(HolderLookup.Provider provider, CompoundTag data) {
        final UUID uuid = data.getUUID("UUID");
        
        final Map<String, StackHandler> entries = new HashMap<>();
        final CompoundTag attachments = data.getCompound("neoforge:attachments");
        final CompoundTag accessoriesInventory = attachments.getCompound("accessories:inventory_holder");
        final CompoundTag containers = accessoriesInventory.getCompound("accessories_containers");
        for (String key : containers.getAllKeys()) {
            final CompoundTag entry = containers.getCompound(key);
            final String identifier = entry.getString("slot_name");
            
            final int size = entry.getInt("current_size");
            final ListTag itemList = entry.getList("items", Tag.TAG_COMPOUND);
            final NonNullList<ItemStack> items = StackHandler.createList(provider, itemList, size);
            final ListTag cosmeticList = entry.getList("cosmetics", Tag.TAG_COMPOUND);
            final NonNullList<ItemStack> cosmetics = StackHandler.createList(provider, cosmeticList, size);
            entries.put(identifier, new StackHandler(items, cosmetics));
        }
        return new AccessoriesContainer(entries);
    }
    
    @Override
    public void write(HolderLookup.Provider provider, CompoundTag data) {
        final CompoundTag attachments = data.getCompound("neoforge:attachments");
        final CompoundTag accessoriesInventory = attachments.getCompound("accessories:inventory_holder");
        final CompoundTag containers = accessoriesInventory.getCompound("accessories_containers");
        for (String key : containers.getAllKeys()) {
            final CompoundTag entry = containers.getCompound(key);
            final StackHandler handler = this.entries.get(key);
            if (handler == null) continue;
            if (handler.isEmpty()) continue;
            
            ListTag stacks = StackHandler.writeList(provider, handler.items());
            entry.put("items", stacks);
            ListTag cosmetics = StackHandler.writeList(provider, handler.cosmetics());
            entry.put("cosmetics", cosmetics);
        }
    }
    
    @Override
    public List<ItemStack> merge(AccessoriesContainer other) {
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
        
        private static NonNullList<ItemStack> createList(HolderLookup.Provider provider, ListTag data, int size) {
            final NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
            for (int i = 0; i < data.size(); i++) {
                CompoundTag tag = data.getCompound(i);
                int slot = tag.getInt("Slot");
                if (slot < 0 || slot >= size) continue;
                ItemStack.parse(provider, tag).ifPresent(stack -> items.set(slot, stack));
            }
            return items;
        }
        
        private static ListTag writeList(HolderLookup.Provider provider, NonNullList<ItemStack> items) {
            ListTag list = new ListTag();
            for (int i = 0; i < items.size(); i++) {
                ItemStack stack = items.get(i).copyAndClear();
                if (stack.isEmpty()) continue;
                CompoundTag tag = new CompoundTag();
                tag.putInt("Slot", i);
                list.add(stack.save(provider, tag));
            }
            return list;
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
