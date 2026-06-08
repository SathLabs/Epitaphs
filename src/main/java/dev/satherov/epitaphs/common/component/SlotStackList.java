package dev.satherov.epitaphs.common.component;

import lombok.Getter;

import net.minecraft.core.NonNullList;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.ItemStack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class SlotStackList implements Iterable<ItemStackWithSlot> {
    
    private static final Codec<SlotStackList> RECORD_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("size").forGetter((SlotStackList list) -> list.size),
            Codec.list(ItemStackWithSlot.CODEC).fieldOf("items").forGetter(SlotStackList::getList)
    ).apply(instance, SlotStackList::new));
    
    public static final Codec<SlotStackList> CODEC = SlotStackList.RECORD_CODEC.withAlternative(NonNullList.codecOf(ItemStack.OPTIONAL_CODEC), SlotStackList::new);
    
    private final int size;
    private final List<ItemStackWithSlot> list = new ArrayList<>();
    private final Map<Integer, ItemStack> slots = new HashMap<>();
    @Getter private int nextFree = 0;
    
    public SlotStackList() {
        this(0);
    }
    
    public SlotStackList(int size) {
        this.size = Math.max(0, size);
    }
    
    public SlotStackList(List<ItemStackWithSlot> list) {
        this(list.stream().mapToInt(ItemStackWithSlot::slot).max().orElse(-1) + 1, list);
    }
    
    public SlotStackList(NonNullList<ItemStack> list) {
        this(list.size(), IntStream.range(0, list.size())
                .mapToObj(slot -> new ItemStackWithSlot(slot, list.get(slot)))
                .toList());
    }
    
    private SlotStackList(int size, List<ItemStackWithSlot> list) {
        this(size);
        list.forEach(this::add);
    }
    
    private void updateNextFree() {
        while (this.nextFree < this.size && this.slots.containsKey(this.nextFree)) {
            this.nextFree++;
        }
    }
    
    public boolean add(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return this.add(this.nextFree, stack);
    }
    
    public boolean add(ItemStackWithSlot entry) {
        if (entry == null) return false;
        return this.add(entry.slot(), entry.stack());
    }
    
    public boolean add(int slot, ItemStack stack) {
        if (slot < 0 || slot >= this.size || stack == null || stack.isEmpty()) return false;
        if (this.slots.putIfAbsent(slot, stack) != null) return false;
        this.list.add(new ItemStackWithSlot(slot, stack));
        if (slot == this.nextFree) this.updateNextFree();
        return true;
    }
    
    public ItemStackWithSlot get(int slot) {
        final ItemStack stack = this.slots.get(slot);
        return stack == null ? null : new ItemStackWithSlot(slot, stack);
    }
    
    public ItemStack getStack(int slot) {
        if (slot < 0 || slot >= this.size) return ItemStack.EMPTY;
        return this.slots.getOrDefault(slot, ItemStack.EMPTY);
    }
    
    public List<ItemStackWithSlot> getList() {
        return this.list.stream()
                .filter(entry -> !entry.stack().isEmpty())
                .toList();
    }
    
    @Override
    public @NonNull Iterator<ItemStackWithSlot> iterator() {
        return this.getList().iterator();
    }
    
    public int size() {
        return this.size;
    }
    
    public boolean isEmpty() {
        return this.list.isEmpty() || this.list.stream().map(ItemStackWithSlot::stack).allMatch(ItemStack::isEmpty);
    }
}
