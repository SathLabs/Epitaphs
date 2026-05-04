package dev.satherov.epitaphs.common.container;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.common.data.SoulboundHandler;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

public record InventoryContainer(NonNullList<ItemStack> items, NonNullList<ItemStack> armor, NonNullList<ItemStack> offhand) implements SaveContainer<InventoryContainer> {
    
    public static final Codec<InventoryContainer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            NonNullList.codecOf(ItemStack.OPTIONAL_CODEC).fieldOf("items").forGetter(InventoryContainer::items),
            NonNullList.codecOf(ItemStack.OPTIONAL_CODEC).fieldOf("armor").forGetter(InventoryContainer::armor),
            NonNullList.codecOf(ItemStack.OPTIONAL_CODEC).fieldOf("offhand").forGetter(InventoryContainer::offhand)
    ).apply(instance, InventoryContainer::new));
    
    private static final int ITEM_SIZE = 36;
    private static final int ARMOR_SIZE = 4;
    private static final int OFFHAND_SIZE = 1;
    
    public static InventoryContainer empty() {
        return new InventoryContainer(
                NonNullList.withSize(InventoryContainer.ITEM_SIZE, ItemStack.EMPTY),
                NonNullList.withSize(InventoryContainer.ARMOR_SIZE, ItemStack.EMPTY),
                NonNullList.withSize(InventoryContainer.OFFHAND_SIZE, ItemStack.EMPTY)
        );
    }
    
    // ==================== ONLINE ====================
    
    ///
    /// Creates an Inventory Container from a ServerPlayer
    ///
    /// @param player ServerPlayer to turn into a container
    ///
    /// @return Inventory Container instance
    ///
    public static InventoryContainer create(ServerPlayer player) {
        final Inventory inventory = player.getInventory();
        
        final NonNullList<ItemStack> items = NonNullList.withSize(InventoryContainer.ITEM_SIZE, ItemStack.EMPTY);
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            items.set(slot, inventory.items.get(slot).copy());
        }
        
        final NonNullList<ItemStack> armor = NonNullList.withSize(InventoryContainer.ARMOR_SIZE, ItemStack.EMPTY);
        for (int slot = 0; slot < inventory.armor.size(); slot++) {
            armor.set(slot, inventory.armor.get(slot).copy());
        }
        
        final NonNullList<ItemStack> offhand = NonNullList.withSize(InventoryContainer.OFFHAND_SIZE, ItemStack.EMPTY);
        for (int slot = 0; slot < inventory.offhand.size(); slot++) {
            offhand.set(slot, inventory.offhand.get(slot).copy());
        }
        
        return new InventoryContainer(items, armor, offhand);
    }
    
    @Override
    public void write(ServerPlayer player) {
        final Inventory inventory = player.getInventory();
        
        for (int slot = 0; slot < this.items.size(); slot++) {
            final ItemStack stack = this.items.get(slot);
            if (stack.isEmpty()) continue;
            inventory.items.set(slot, stack.copyAndClear());
        }
        
        for (int slot = 0; slot < this.armor.size(); slot++) {
            final ItemStack stack = this.armor.get(slot);
            if (stack.isEmpty()) continue;
            inventory.armor.set(slot, stack.copyAndClear());
        }
        
        for (int slot = 0; slot < this.offhand.size(); slot++) {
            final ItemStack stack = this.offhand.get(slot);
            if (stack.isEmpty()) continue;
            inventory.offhand.set(slot, stack.copyAndClear());
        }
    }
    
    public static InventoryContainer createSoulbound(ServerPlayer player) {
        final Inventory inventory = player.getInventory();
        
        final NonNullList<ItemStack> items = NonNullList.withSize(InventoryContainer.ITEM_SIZE, ItemStack.EMPTY);
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            ItemStack stack = inventory.items.get(slot);
            if (SoulboundHandler.isSoulbound(stack)) items.set(slot, stack.copyAndClear());
        }
        
        final NonNullList<ItemStack> armor = NonNullList.withSize(InventoryContainer.ARMOR_SIZE, ItemStack.EMPTY);
        for (int slot = 0; slot < inventory.armor.size(); slot++) {
            ItemStack stack = inventory.armor.get(slot);
            if (SoulboundHandler.isSoulbound(stack)) armor.set(slot, stack.copyAndClear());
        }
        
        final NonNullList<ItemStack> offhand = NonNullList.withSize(InventoryContainer.OFFHAND_SIZE, ItemStack.EMPTY);
        for (int slot = 0; slot < inventory.offhand.size(); slot++) {
            ItemStack stack = inventory.offhand.get(slot);
            if (SoulboundHandler.isSoulbound(stack)) offhand.set(slot, stack.copyAndClear());
        }
        
        return new InventoryContainer(items, armor, offhand);
    }
    
    // ==================== OFFLINE ====================
    
    ///
    /// Creates an Inventory Container from a player data tag
    ///
    /// @param provider Registry Access used to parse ItemStacks
    /// @param data   player data compound tag
    ///
    /// @return Inventory Container instance
    ///
    @SuppressWarnings("ConstantValue")
    public static InventoryContainer create(HolderLookup.Provider provider, CompoundTag data) {
        final ListTag inventory = data.getList("Inventory", Tag.TAG_COMPOUND);
        final NonNullList<ItemStack> items = NonNullList.withSize(InventoryContainer.ITEM_SIZE, ItemStack.EMPTY);
        final NonNullList<ItemStack> armor = NonNullList.withSize(InventoryContainer.ARMOR_SIZE, ItemStack.EMPTY);
        final NonNullList<ItemStack> offhand = NonNullList.withSize(InventoryContainer.OFFHAND_SIZE, ItemStack.EMPTY);
        
        for (int i = 0; i < inventory.size(); i++) {
            CompoundTag entry = inventory.getCompound(i);
            int slot = entry.getByte("Slot") & 255;
            ItemStack stack = ItemStack.parse(provider, entry).orElse(ItemStack.EMPTY);
            if (stack.isEmpty()) continue;
            
            if (slot >= 0 && slot < InventoryContainer.ITEM_SIZE) {
                items.set(slot, stack);
            } else if (slot >= 100 && slot < InventoryContainer.ARMOR_SIZE + 100) {
                armor.set(slot - 100, stack);
            } else if (slot >= 150 && slot < InventoryContainer.OFFHAND_SIZE + 150) {
                offhand.set(slot - 150, stack);
            }
        }
        
        return new InventoryContainer(items, armor, offhand);
    }
    
    @Override
    public void write(HolderLookup.Provider provider, CompoundTag data) {
        final ListTag inventory = new ListTag();
        
        for (int slot = 0; slot < this.items.size(); slot++) {
            ItemStack stack = this.items.get(slot);
            CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte) slot);
            inventory.add(stack.save(provider, entry));
        }
        
        for (int slot = 0; slot < this.armor.size(); slot++) {
            ItemStack stack = this.armor.get(slot);
            CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte) (slot + 100));
            inventory.add(stack.save(provider, entry));
        }
        
        for (int slot = 0; slot < this.offhand.size(); slot++) {
            ItemStack stack = this.offhand.get(slot);
            CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte) (slot + 150));
            inventory.add(stack.save(provider, entry));
        }
        
        data.put("Inventory", inventory);
    }
    
    // ==================== OTHER ====================
    
    @Override
    public List<ItemStack> merge(InventoryContainer other) {
        final List<ItemStack> overflow = new ArrayList<>();
        
        for (int slot = 0; slot < InventoryContainer.ITEM_SIZE; slot++) {
            final ItemStack stack = other.items.get(slot).copyAndClear();
            final ItemStack existing = this.items.get(slot);
            if (existing.isEmpty()) this.items.set(slot, stack);
            else overflow.add(stack);
        }
        
        for (int slot = 0; slot < InventoryContainer.ARMOR_SIZE; slot++) {
            final ItemStack stack = other.armor.get(slot).copyAndClear();
            final ItemStack existing = this.armor.get(slot);
            if (existing.isEmpty()) this.armor.set(slot, stack);
            else overflow.add(stack);
        }
        
        for (int slot = 0; slot < InventoryContainer.OFFHAND_SIZE; slot++) {
            final ItemStack stack = other.offhand.get(slot).copyAndClear();
            final ItemStack existing = this.offhand.get(slot);
            if (existing.isEmpty()) this.offhand.set(slot, stack);
            else overflow.add(stack);
        }
        
        return overflow;
    }
    
    @Override
    public List<ItemStack> gather() {
        final List<ItemStack> result = new ArrayList<>();
        this.items.forEach(stack -> result.add(stack.copyAndClear()));
        this.armor.forEach(stack -> result.add(stack.copyAndClear()));
        this.offhand.forEach(stack -> result.add(stack.copyAndClear()));
        return result;
    }
    
    @Override
    public boolean isEmpty() {
        return (this.items.isEmpty() || this.items.stream().allMatch(ItemStack::isEmpty)) &&
                (this.armor.isEmpty() || this.armor.stream().allMatch(ItemStack::isEmpty)) &&
                (this.offhand.isEmpty() || this.offhand.stream().allMatch(ItemStack::isEmpty));
    }
    
    public List<ItemStack> insert(List<ItemStack> stacks) {
        final List<ItemStack> overflow = new ArrayList<>();
        
        for (ItemStack input : stacks) {
            if (input.isEmpty()) continue;
            ItemStack stack = input.copyAndClear();
            
            // try to insert into any matching
            for (int slot = 0; slot < InventoryContainer.ITEM_SIZE; slot++) {
                if (stack.isEmpty()) break;
                
                final ItemStack existing = this.items.get(slot);
                if (existing.isEmpty()) continue;
                if (!existing.isStackable()) continue;
                if (!ItemStack.isSameItemSameComponents(existing, stack)) continue;
                
                final int space = existing.getMaxStackSize() - existing.getCount();
                if (space <= 0) continue;
                
                final int moved = Math.min(space, stack.getCount());
                existing.grow(moved);
                stack.shrink(moved);
            }
            
            if (stack.isEmpty()) continue;
            
            // try to insert into any empty
            for (int slot = 0; slot < InventoryContainer.ITEM_SIZE; slot++) {
                final ItemStack existing = this.items.get(slot);
                if (!existing.isEmpty()) continue;
                
                this.items.set(slot, stack.copyAndClear());
                break;
            }
            
            if (!stack.isEmpty()) overflow.add(stack.copyAndClear());
        }
        
        return overflow;
    }
}
