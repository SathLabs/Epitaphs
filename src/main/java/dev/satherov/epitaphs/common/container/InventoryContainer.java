package dev.satherov.epitaphs.common.container;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.common.data.SoulboundHandler;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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
    
    private static final int ARMOR_SIZE = 4;
    private static final int OFFHAND_SIZE = 1;
    
    public static InventoryContainer empty() {
        return new InventoryContainer(
                NonNullList.withSize(Inventory.INVENTORY_SIZE, ItemStack.EMPTY),
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
        
        final NonNullList<ItemStack> items = NonNullList.withSize(Inventory.INVENTORY_SIZE, ItemStack.EMPTY);
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            items.set(slot, inventory.getItem(slot).copy());
        }
        
        final NonNullList<ItemStack> armor = NonNullList.withSize(InventoryContainer.ARMOR_SIZE, ItemStack.EMPTY);
        for (int slot = 0; slot < InventoryContainer.ARMOR_SIZE; slot++) {
            armor.set(slot, inventory.getItem(Inventory.INVENTORY_SIZE + slot).copy());
        }
        
        final NonNullList<ItemStack> offhand = NonNullList.withSize(InventoryContainer.OFFHAND_SIZE, ItemStack.EMPTY);
        for (int slot = 0; slot < InventoryContainer.OFFHAND_SIZE; slot++) {
            offhand.set(slot, inventory.getItem(Inventory.SLOT_OFFHAND + slot).copy());
        }
        
        return new InventoryContainer(items, armor, offhand);
    }
    
    @Override
    public void write(ServerPlayer player) {
        final Inventory inventory = player.getInventory();
        
        for (int slot = 0; slot < this.items.size(); slot++) {
            final ItemStack stack = this.items.get(slot);
            if (stack.isEmpty()) continue;
            inventory.setItem(slot, stack.copyAndClear());
        }
        
        for (int slot = 0; slot < this.armor.size(); slot++) {
            final ItemStack stack = this.armor.get(slot);
            if (stack.isEmpty()) continue;
            inventory.setItem(Inventory.INVENTORY_SIZE + slot, stack.copyAndClear());
        }
        
        for (int slot = 0; slot < this.offhand.size(); slot++) {
            final ItemStack stack = this.offhand.get(slot);
            if (stack.isEmpty()) continue;
            inventory.setItem(Inventory.SLOT_OFFHAND + slot, stack.copyAndClear());
        }
    }
    
    public static InventoryContainer createSoulbound(ServerPlayer player) {
        final Inventory inventory = player.getInventory();
        
        final NonNullList<ItemStack> items = NonNullList.withSize(Inventory.INVENTORY_SIZE, ItemStack.EMPTY);
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (SoulboundHandler.isSoulbound(stack)) items.set(slot, stack.copyAndClear());
        }
        
        final NonNullList<ItemStack> armor = NonNullList.withSize(InventoryContainer.ARMOR_SIZE, ItemStack.EMPTY);
        for (int slot = 0; slot < InventoryContainer.ARMOR_SIZE; slot++) {
            ItemStack stack = inventory.getItem(Inventory.INVENTORY_SIZE + slot);
            if (SoulboundHandler.isSoulbound(stack)) armor.set(slot, stack.copyAndClear());
        }
        
        final NonNullList<ItemStack> offhand = NonNullList.withSize(InventoryContainer.OFFHAND_SIZE, ItemStack.EMPTY);
        for (int slot = 0; slot < InventoryContainer.OFFHAND_SIZE; slot++) {
            ItemStack stack = inventory.getItem(Inventory.SLOT_OFFHAND + slot);
            if (SoulboundHandler.isSoulbound(stack)) offhand.set(slot, stack.copyAndClear());
        }
        
        return new InventoryContainer(items, armor, offhand);
    }
    
    // ==================== OFFLINE ====================
    
    ///
    /// Creates an Inventory Container from player data
    ///
    /// @param input player data input
    ///
    /// @return Inventory Container instance
    ///
    public static InventoryContainer create(ValueInput input) {
        final NonNullList<ItemStack> items = NonNullList.withSize(Inventory.INVENTORY_SIZE, ItemStack.EMPTY);
        final NonNullList<ItemStack> armor = NonNullList.withSize(InventoryContainer.ARMOR_SIZE, ItemStack.EMPTY);
        final NonNullList<ItemStack> offhand = NonNullList.withSize(InventoryContainer.OFFHAND_SIZE, ItemStack.EMPTY);
        
        input.listOrEmpty("Inventory", ItemStackWithSlot.CODEC).forEach(entry -> {
            final int slot = entry.slot();
            final ItemStack stack = entry.stack();
            if (stack.isEmpty()) return;
            
            if (slot >= 0 && slot < Inventory.INVENTORY_SIZE) items.set(slot, stack);
        });
        
        final EntityEquipment equipment = input.read("equipment", EntityEquipment.CODEC).orElseGet(EntityEquipment::new);
        armor.set(0, equipment.get(EquipmentSlot.FEET));
        armor.set(1, equipment.get(EquipmentSlot.LEGS));
        armor.set(2, equipment.get(EquipmentSlot.CHEST));
        armor.set(3, equipment.get(EquipmentSlot.HEAD));
        offhand.set(0, equipment.get(EquipmentSlot.OFFHAND));
        
        return new InventoryContainer(items, armor, offhand);
    }
    
    @Override
    public void write(ValueInput input, ValueOutput output) {
        final ValueOutput.TypedOutputList<ItemStackWithSlot> inventory = output.list("Inventory", ItemStackWithSlot.CODEC);
        
        for (int slot = 0; slot < this.items.size(); slot++) {
            ItemStack stack = this.items.get(slot);
            if (stack.isEmpty()) continue;
            inventory.add(new ItemStackWithSlot(slot, stack));
        }
        
        final EntityEquipment equipment = input.read("equipment", EntityEquipment.CODEC).orElseGet(EntityEquipment::new);
        equipment.set(EquipmentSlot.FEET, this.armor.get(0));
        equipment.set(EquipmentSlot.LEGS, this.armor.get(1));
        equipment.set(EquipmentSlot.CHEST, this.armor.get(2));
        equipment.set(EquipmentSlot.HEAD, this.armor.get(3));
        equipment.set(EquipmentSlot.OFFHAND, this.offhand.getFirst());
        output.store("equipment", EntityEquipment.CODEC, equipment);
    }
    
    // ==================== OTHER ====================
    
    @Override
    public List<ItemStack> merge(InventoryContainer other) {
        final List<ItemStack> overflow = new ArrayList<>();
        
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
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
            for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
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
            for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
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
