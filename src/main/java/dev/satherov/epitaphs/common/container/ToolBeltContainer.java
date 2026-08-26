package dev.satherov.epitaphs.common.container;

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

import dev.gigaherz.toolbelt.slot.BeltAttachment;

import java.util.ArrayList;
import java.util.List;

public record ToolBeltContainer(NonNullList<ItemStack> items) implements SaveContainer<ToolBeltContainer> {
    
    public static final Codec<ToolBeltContainer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            NonNullList.codecOf(ItemStack.OPTIONAL_CODEC).fieldOf("items").forGetter(ToolBeltContainer::items)
    ).apply(instance, ToolBeltContainer::new));
    
    public static ToolBeltContainer empty() {
        return new ToolBeltContainer(NonNullList.withSize(1, ItemStack.EMPTY));
    }
    
    // ==================== ONLINE ====================
    
    public static ToolBeltContainer create(ServerPlayer player) {
        final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
        items.set(0, BeltAttachment.get(player).getContents().copy());
        return new ToolBeltContainer(items);
    }
    
    @Override
    public void write(ServerPlayer player) {
        final ItemStack stack = this.items.getFirst().copyAndClear();
        if (!stack.isEmpty()) BeltAttachment.get(player).setContents(stack);
    }
    
    public static ToolBeltContainer createSoulbound(ServerPlayer player) {
        final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
        final BeltAttachment attachment = BeltAttachment.get(player);
        final ItemStack stack = attachment.getContents();
        if (SoulboundHandler.isSoulbound(stack)) {
            items.set(0, stack.copy());
            attachment.setContents(ItemStack.EMPTY);
        }
        
        return new ToolBeltContainer(items);
    }
    
    // ==================== OFFLINE ====================
    
    public static ToolBeltContainer create(HolderLookup.Provider provider, CompoundTag data) {
        final CompoundTag attachments = data.getCompound("neoforge:attachments");
        final CompoundTag belt = attachments.getCompound("toolbelt:belt");
        final int size = Math.max(belt.getInt("Size"), 1);
        final NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
        
        final ListTag list = belt.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            final CompoundTag entry = list.getCompound(i);
            final int slot = entry.getInt("Slot");
            if (slot < 0 || slot >= size) continue;
            ItemStack.parse(provider, entry).ifPresent(stack -> items.set(slot, stack));
        }
        
        return new ToolBeltContainer(items);
    }
    
    @Override
    public void write(HolderLookup.Provider provider, CompoundTag data) {
        final ItemStack stack = this.items.getFirst().copyAndClear();
        if (stack.isEmpty()) return;
        
        final CompoundTag attachments = data.getCompound("neoforge:attachments");
        final CompoundTag belt = attachments.getCompound("toolbelt:belt");
        final ListTag list = new ListTag();
        final CompoundTag entry = new CompoundTag();
        entry.putInt("Slot", 0);
        list.add(stack.save(provider, entry));
        belt.put("Items", list);
        belt.putInt("Size", Math.max(belt.getInt("Size"), 1));
        attachments.put("toolbelt:belt", belt);
        data.put("neoforge:attachments", attachments);
    }
    
    // ==================== OTHER ====================
    
    @Override
    public List<ItemStack> merge(ToolBeltContainer other) {
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
        
        return overflow;
    }
    
    @Override
    public List<ItemStack> gather() {
        final List<ItemStack> result = new ArrayList<>();
        this.items.forEach(stack -> result.add(stack.copyAndClear()));
        return result;
    }
    
    @Override
    public boolean isEmpty() {
        return this.items.isEmpty() || this.items.stream().allMatch(ItemStack::isEmpty);
    }
}
