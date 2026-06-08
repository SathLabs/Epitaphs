package dev.satherov.epitaphs.common.container;

import dev.satherov.epitaphs.common.component.SlotStackList;
import dev.satherov.epitaphs.common.data.SoulboundHandler;

import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

record StackHandler(SlotStackList items, SlotStackList cosmetics) {
    
    static final Codec<StackHandler> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SlotStackList.CODEC.fieldOf("items").forGetter(StackHandler::items),
            SlotStackList.CODEC.fieldOf("cosmetics").forGetter(StackHandler::cosmetics)
    ).apply(instance, StackHandler::new));
    
    static StackHandler create(int itemSize, IntFunction<ItemStack> itemGetter, int cosmeticSize, IntFunction<ItemStack> cosmeticGetter) {
        final SlotStackList items = new SlotStackList(itemSize);
        final SlotStackList cosmetics = new SlotStackList(cosmeticSize);
        
        IntStream.range(0, itemSize).forEach(slot -> items.add(slot, itemGetter.apply(slot).copy()));
        IntStream.range(0, cosmeticSize).forEach(slot -> cosmetics.add(slot, cosmeticGetter.apply(slot).copy()));
        
        return new StackHandler(items, cosmetics);
    }
    
    static StackHandler createSoulbound(int itemSize, IntFunction<ItemStack> itemGetter, int cosmeticSize, IntFunction<ItemStack> cosmeticGetter) {
        final SlotStackList items = new SlotStackList(itemSize);
        final SlotStackList cosmetics = new SlotStackList(cosmeticSize);
        
        IntStream.range(0, itemSize).forEach(slot -> {
            final ItemStack stack = itemGetter.apply(slot);
            if (SoulboundHandler.isSoulbound(stack)) items.add(slot, stack.copyAndClear());
        });
        IntStream.range(0, cosmeticSize).forEach(slot -> {
            final ItemStack stack = cosmeticGetter.apply(slot);
            if (SoulboundHandler.isSoulbound(stack)) cosmetics.add(slot, stack.copyAndClear());
        });
        
        return new StackHandler(items, cosmetics);
    }
    
    static StackHandler create(ValueInput input) {
        final SlotStackList items = StackHandler.createList(input.childOrEmpty("Stacks"));
        final SlotStackList cosmetics = StackHandler.createList(input.childOrEmpty("Cosmetics"));
        return new StackHandler(items, cosmetics);
    }
    
    private static SlotStackList createList(ValueInput input) {
        final SlotStackList items = new SlotStackList(input.getIntOr("Size", 0));
        input.listOrEmpty("Items", ItemStackWithSlot.CODEC).forEach(items::add);
        return items;
    }
    
    private static void writeList(ValueOutput output, SlotStackList items) {
        final ValueOutput.TypedOutputList<ItemStackWithSlot> list = output.list("Items", ItemStackWithSlot.CODEC);
        items.forEach(list::add);
        output.putInt("Size", items.size());
    }
    
    void write(ValueOutput output) {
        StackHandler.writeList(output.child("Stacks"), this.items);
        StackHandler.writeList(output.child("Cosmetics"), this.cosmetics);
    }
    
    List<ItemStack> merge(StackHandler other) {
        final List<ItemStack> overflow = new ArrayList<>();
        other.items.forEach(entry -> {
            final ItemStack stack = entry.stack().copyAndClear();
            if (!stack.isEmpty() && !this.items.add(entry.slot(), stack)) overflow.add(stack);
        });
        other.cosmetics.forEach(entry -> {
            final ItemStack stack = entry.stack().copyAndClear();
            if (!stack.isEmpty() && !this.cosmetics.add(entry.slot(), stack)) overflow.add(stack);
        });
        return overflow;
    }
    
    List<ItemStack> gather() {
        final List<ItemStack> result = new ArrayList<>();
        this.items.forEach(entry -> result.add(entry.stack().copyAndClear()));
        this.cosmetics.forEach(entry -> result.add(entry.stack().copyAndClear()));
        return result;
    }
    
    boolean isEmpty() {
        return this.items.isEmpty() && this.cosmetics.isEmpty();
    }
}
