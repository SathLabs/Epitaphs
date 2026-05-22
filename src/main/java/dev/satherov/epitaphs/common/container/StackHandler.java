package dev.satherov.epitaphs.common.container;

import dev.satherov.epitaphs.common.data.SoulboundHandler;

import net.minecraft.core.NonNullList;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

record StackHandler(NonNullList<ItemStack> items, NonNullList<ItemStack> cosmetics) {
    
    static final Codec<StackHandler> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            NonNullList.codecOf(ItemStack.OPTIONAL_CODEC).fieldOf("items").forGetter(StackHandler::items),
            NonNullList.codecOf(ItemStack.OPTIONAL_CODEC).fieldOf("cosmetics").forGetter(StackHandler::cosmetics)
    ).apply(instance, StackHandler::new));
    
    static StackHandler create(int itemSize, IntFunction<ItemStack> itemGetter, int cosmeticSize, IntFunction<ItemStack> cosmeticGetter) {
        final NonNullList<ItemStack> items = NonNullList.withSize(itemSize, ItemStack.EMPTY);
        for (int slot = 0; slot < itemSize; slot++) {
            items.set(slot, itemGetter.apply(slot).copy());
        }
        
        final NonNullList<ItemStack> cosmetics = NonNullList.withSize(cosmeticSize, ItemStack.EMPTY);
        for (int slot = 0; slot < cosmeticSize; slot++) {
            cosmetics.set(slot, cosmeticGetter.apply(slot).copy());
        }
        
        return new StackHandler(items, cosmetics);
    }
    
    static StackHandler createSoulbound(int itemSize, IntFunction<ItemStack> itemGetter, int cosmeticSize, IntFunction<ItemStack> cosmeticGetter) {
        final NonNullList<ItemStack> items = NonNullList.withSize(itemSize, ItemStack.EMPTY);
        for (int slot = 0; slot < itemSize; slot++) {
            ItemStack stack = itemGetter.apply(slot);
            if (SoulboundHandler.isSoulbound(stack)) items.set(slot, stack.copyAndClear());
        }
        
        final NonNullList<ItemStack> cosmetics = NonNullList.withSize(cosmeticSize, ItemStack.EMPTY);
        for (int slot = 0; slot < cosmeticSize; slot++) {
            ItemStack stack = cosmeticGetter.apply(slot);
            if (SoulboundHandler.isSoulbound(stack)) cosmetics.set(slot, stack.copyAndClear());
        }
        
        return new StackHandler(items, cosmetics);
    }
    
    static StackHandler create(ValueInput input) {
        final NonNullList<ItemStack> items = StackHandler.createList(input.childOrEmpty("Stacks"));
        final NonNullList<ItemStack> cosmetics = StackHandler.createList(input.childOrEmpty("Cosmetics"));
        return new StackHandler(items, cosmetics);
    }
    
    private static NonNullList<ItemStack> createList(ValueInput input) {
        final int size = input.getIntOr("Size", 0);
        final NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
        input.listOrEmpty("Items", ItemStackWithSlot.CODEC).forEach(entry -> {
            if (entry.isValidInContainer(size)) items.set(entry.slot(), entry.stack());
        });
        return items;
    }
    
    private static void writeList(ValueOutput output, NonNullList<ItemStack> items) {
        final ValueOutput.TypedOutputList<ItemStackWithSlot> list = output.list("Items", ItemStackWithSlot.CODEC);
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) continue;
            list.add(new ItemStackWithSlot(slot, stack));
        }
        output.putInt("Size", items.size());
    }
    
    void write(ValueOutput output) {
        StackHandler.writeList(output.child("Stacks"), this.items);
        StackHandler.writeList(output.child("Cosmetics"), this.cosmetics);
    }
    
    List<ItemStack> merge(StackHandler other) {
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
    
    List<ItemStack> gather() {
        final List<ItemStack> result = new ArrayList<>();
        this.items.forEach(stack -> result.add(stack.copyAndClear()));
        this.cosmetics.forEach(stack -> result.add(stack.copyAndClear()));
        return result;
    }
    
    boolean isEmpty() {
        return (this.items.isEmpty() || this.items.stream().allMatch(ItemStack::isEmpty)) &&
                (this.cosmetics.isEmpty() || this.cosmetics.stream().allMatch(ItemStack::isEmpty));
    }
}
