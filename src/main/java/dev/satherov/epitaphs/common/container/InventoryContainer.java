package dev.satherov.epitaphs.common.container;

import dev.satherov.epitaphs.common.component.SlotStackList;
import dev.satherov.epitaphs.common.data.SoulboundHandler;

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
import java.util.stream.IntStream;

public record InventoryContainer(SlotStackList items, SlotStackList armor, SlotStackList offhand) implements SaveContainer<InventoryContainer> {
    
    public static final Codec<InventoryContainer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SlotStackList.CODEC.fieldOf("items").forGetter(InventoryContainer::items),
            SlotStackList.CODEC.fieldOf("armor").forGetter(InventoryContainer::armor),
            SlotStackList.CODEC.fieldOf("offhand").forGetter(InventoryContainer::offhand)
    ).apply(instance, InventoryContainer::new));
    
    private static final int ARMOR_SIZE = 4;
    private static final int OFFHAND_SIZE = 1;
    
    public static InventoryContainer empty() {
        return new InventoryContainer(
                new SlotStackList(Inventory.INVENTORY_SIZE),
                new SlotStackList(InventoryContainer.ARMOR_SIZE),
                new SlotStackList(InventoryContainer.OFFHAND_SIZE)
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
        final SlotStackList items = new SlotStackList(Inventory.INVENTORY_SIZE);
        final SlotStackList armor = new SlotStackList(InventoryContainer.ARMOR_SIZE);
        final SlotStackList offhand = new SlotStackList(InventoryContainer.OFFHAND_SIZE);
        
        IntStream.range(0, Inventory.INVENTORY_SIZE).forEach(slot -> items.add(slot, inventory.getItem(slot).copy()));
        IntStream.range(0, InventoryContainer.ARMOR_SIZE).forEach(slot -> armor.add(slot, inventory.getItem(Inventory.INVENTORY_SIZE + slot).copy()));
        IntStream.range(0, InventoryContainer.OFFHAND_SIZE).forEach(slot -> offhand.add(slot, inventory.getItem(Inventory.SLOT_OFFHAND + slot).copy()));
        
        return new InventoryContainer(items, armor, offhand);
    }
    
    public static InventoryContainer createSoulbound(ServerPlayer player) {
        final Inventory inventory = player.getInventory();
        final SlotStackList items = new SlotStackList(Inventory.INVENTORY_SIZE);
        final SlotStackList armor = new SlotStackList(InventoryContainer.ARMOR_SIZE);
        final SlotStackList offhand = new SlotStackList(InventoryContainer.OFFHAND_SIZE);
        
        IntStream.range(0, Inventory.INVENTORY_SIZE).forEach(slot -> {
            final ItemStack stack = inventory.getItem(slot);
            if (SoulboundHandler.isSoulbound(stack)) items.add(slot, stack.copyAndClear());
        });
        IntStream.range(0, InventoryContainer.ARMOR_SIZE).forEach(slot -> {
            final ItemStack stack = inventory.getItem(Inventory.INVENTORY_SIZE + slot);
            if (SoulboundHandler.isSoulbound(stack)) armor.add(slot, stack.copyAndClear());
        });
        IntStream.range(0, InventoryContainer.OFFHAND_SIZE).forEach(slot -> {
            final ItemStack stack = inventory.getItem(Inventory.SLOT_OFFHAND + slot);
            if (SoulboundHandler.isSoulbound(stack)) offhand.add(slot, stack.copyAndClear());
        });
        
        return new InventoryContainer(items, armor, offhand);
    }
    
    ///
    /// Creates an Inventory Container from player data
    ///
    /// @param input player data input
    ///
    /// @return Inventory Container instance
    ///
    public static InventoryContainer create(ValueInput input) {
        final SlotStackList items = new SlotStackList(Inventory.INVENTORY_SIZE);
        final SlotStackList armor = new SlotStackList(InventoryContainer.ARMOR_SIZE);
        final SlotStackList offhand = new SlotStackList(InventoryContainer.OFFHAND_SIZE);
        
        input.listOrEmpty("Inventory", ItemStackWithSlot.CODEC).forEach(items::add);
        
        final EntityEquipment equipment = input.read("equipment", EntityEquipment.CODEC).orElseGet(EntityEquipment::new);
        armor.add(0, equipment.get(EquipmentSlot.FEET));
        armor.add(1, equipment.get(EquipmentSlot.LEGS));
        armor.add(2, equipment.get(EquipmentSlot.CHEST));
        armor.add(3, equipment.get(EquipmentSlot.HEAD));
        offhand.add(0, equipment.get(EquipmentSlot.OFFHAND));
        
        return new InventoryContainer(items, armor, offhand);
    }
    
    // ==================== OFFLINE ====================
    
    @Override
    public void write(ServerPlayer player) {
        final Inventory inventory = player.getInventory();
        
        this.items.forEach(entry -> inventory.setItem(entry.slot(), entry.stack().copyAndClear()));
        this.armor.forEach(entry -> inventory.setItem(Inventory.INVENTORY_SIZE + entry.slot(), entry.stack().copyAndClear()));
        this.offhand.forEach(entry -> inventory.setItem(Inventory.SLOT_OFFHAND + entry.slot(), entry.stack().copyAndClear()));
    }
    
    @Override
    public void write(ValueInput input, ValueOutput output) {
        final ValueOutput.TypedOutputList<ItemStackWithSlot> inventory = output.list("Inventory", ItemStackWithSlot.CODEC);
        this.items.forEach(inventory::add);
        
        final EntityEquipment equipment = input.read("equipment", EntityEquipment.CODEC).orElseGet(EntityEquipment::new);
        equipment.set(EquipmentSlot.FEET, this.armor.getStack(0));
        equipment.set(EquipmentSlot.LEGS, this.armor.getStack(1));
        equipment.set(EquipmentSlot.CHEST, this.armor.getStack(2));
        equipment.set(EquipmentSlot.HEAD, this.armor.getStack(3));
        equipment.set(EquipmentSlot.OFFHAND, this.offhand.getStack(0));
        output.store("equipment", EntityEquipment.CODEC, equipment);
    }
    
    // ==================== OTHER ====================
    
    @Override
    public List<ItemStack> merge(InventoryContainer other) {
        final List<ItemStack> overflow = new ArrayList<>();
        
        other.items.forEach(entry -> {
            final ItemStack stack = entry.stack().copyAndClear();
            if (!stack.isEmpty() && !this.items.add(entry.slot(), stack)) overflow.add(stack);
        });
        other.armor.forEach(entry -> {
            final ItemStack stack = entry.stack().copyAndClear();
            if (!stack.isEmpty() && !this.armor.add(entry.slot(), stack)) overflow.add(stack);
        });
        other.offhand.forEach(entry -> {
            final ItemStack stack = entry.stack().copyAndClear();
            if (!stack.isEmpty() && !this.offhand.add(entry.slot(), stack)) overflow.add(stack);
        });
        
        return overflow;
    }
    
    @Override
    public List<ItemStack> gather() {
        final List<ItemStack> result = new ArrayList<>();
        this.items.forEach(entry -> result.add(entry.stack().copyAndClear()));
        this.armor.forEach(entry -> result.add(entry.stack().copyAndClear()));
        this.offhand.forEach(entry -> result.add(entry.stack().copyAndClear()));
        return result;
    }
    
    @Override
    public boolean isEmpty() {
        return this.items.isEmpty() && this.armor.isEmpty() && this.offhand.isEmpty();
    }
    
    public List<ItemStack> insert(List<ItemStack> stacks) {
        final List<ItemStack> overflow = new ArrayList<>();
        
        stacks.stream().filter(stack -> !stack.isEmpty()).forEach(input -> {
            final ItemStack stack = input.copyAndClear();
            
            this.items.forEach(entry -> {
                final ItemStack existing = entry.stack();
                if (stack.isEmpty()) return;
                if (!existing.isStackable()) return;
                if (!ItemStack.isSameItemSameComponents(existing, stack)) return;
                
                final int space = existing.getMaxStackSize() - existing.getCount();
                if (space <= 0) return;
                
                final int moved = Math.min(space, stack.getCount());
                existing.grow(moved);
                stack.shrink(moved);
            });
            
            if (!stack.isEmpty()) {
                final ItemStack remaining = stack.copyAndClear();
                if (!this.items.add(remaining)) overflow.add(remaining);
            }
        });
        
        return overflow;
    }
}
