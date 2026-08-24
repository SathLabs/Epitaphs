package dev.satherov.epitaphs.common.container;

import dev.satherov.epitaphs.common.data.SoulboundHandler;
import dev.satherov.epitaphs.compat.CosmeticArmorHandler;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import lain.mods.cos.api.CosArmorAPI;
import lain.mods.cos.api.inventory.CAStacksBase;

import java.util.ArrayList;
import java.util.List;


public record CosmeticArmorContainer(NonNullList<ItemStack> items) implements SaveContainer<CosmeticArmorContainer> {
    
    public static final Codec<CosmeticArmorContainer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            NonNullList.codecOf(ItemStack.OPTIONAL_CODEC).fieldOf("items").forGetter(CosmeticArmorContainer::items)
    ).apply(instance, CosmeticArmorContainer::new));
    
    private static final int MIN_SIZE = 11;
    
    public static CosmeticArmorContainer empty() {
        return new CosmeticArmorContainer(NonNullList.withSize(CosmeticArmorContainer.MIN_SIZE, ItemStack.EMPTY));
    }
    
    // ==================== ONLINE ====================
   
    
    public static CosmeticArmorContainer create(ServerPlayer player) {
        final CAStacksBase stacks = CosArmorAPI.getCAStacks(player.getUUID());
        final NonNullList<ItemStack> items = NonNullList.withSize(stacks.getSlots(), ItemStack.EMPTY);
        for (int slot = 0; slot < stacks.getSlots(); slot++) {
            items.set(slot, stacks.getStackInSlot(slot).copy());
        }
        
        return new CosmeticArmorContainer(items);
    }
    
    @Override
    public void write(ServerPlayer player) {
        final CAStacksBase stacks = CosArmorAPI.getCAStacks(player.getUUID());
        for (int slot = 0; slot < this.items.size(); slot++) {
            final ItemStack stack = this.items.get(slot).copyAndClear();
            if (stack.isEmpty()) continue;
            if (slot < stacks.getSlots()) {
                stacks.setStackInSlot(slot, stack);
                continue;
            }
            
            for (int i = 0; i < stacks.getSlots(); i++) {
                if (stacks.getStackInSlot(i).isEmpty()) {
                    stacks.setStackInSlot(i, stack);
                    break;
                }
            }
        }
    }
    
    ///
    /// Pulls every soulbound stack out of the cosmetic armor of a ServerPlayer and into a container.
    ///
    /// @param player ServerPlayer to take the stacks from
    ///
    /// @return Cosmetic Armor Container instance
    ///
    public static CosmeticArmorContainer createSoulbound(ServerPlayer player) {
        final CAStacksBase stacks = CosArmorAPI.getCAStacks(player.getUUID());
        final NonNullList<ItemStack> items = NonNullList.withSize(stacks.getSlots(), ItemStack.EMPTY);
        for (int slot = 0; slot < stacks.getSlots(); slot++) {
            final ItemStack stack = stacks.getStackInSlot(slot);
            if (SoulboundHandler.isSoulbound(stack)) items.set(slot, stack.copyAndClear());
        }
        
        return new CosmeticArmorContainer(items);
    }
    
    // ==================== OFFLINE ====================
    
    ///
    /// Creates a Cosmetic Armor Container from a player data tag
    ///
    /// @param provider Registry Access used to parse ItemStacks
    /// @param data     player data compound tag
    ///
    /// @return Cosmetic Armor Container instance
    ///
    public static CosmeticArmorContainer create(HolderLookup.Provider provider, CompoundTag data) {
        final CompoundTag stacks = data.getCompound(CosmeticArmorHandler.KEY);
        final int size = Math.max(stacks.getInt("Size"), CosmeticArmorContainer.MIN_SIZE);
        final NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
        
        final ListTag list = stacks.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            final CompoundTag entry = list.getCompound(i);
            final int slot = entry.getInt("Slot");
            if (slot < 0 || slot >= size) continue;
            
            // an entry without an item is a slot that carries nothing but its skin armor flag
            if (!entry.contains("id")) continue;
            
            ItemStack.parse(provider, entry).ifPresent(stack -> items.set(slot, stack));
        }
        
        return new CosmeticArmorContainer(items);
    }
    
    @Override
    public void write(HolderLookup.Provider provider, CompoundTag data) {
        final CompoundTag stacks = data.getCompound(CosmeticArmorHandler.KEY);
        final ListTag previous = stacks.getList("Items", Tag.TAG_COMPOUND);
        
        // the skin armor flag of a slot shares an entry with the item in it, so rewriting the list would drop it
        final boolean[] skins = new boolean[this.items.size()];
        for (int i = 0; i < previous.size(); i++) {
            final CompoundTag entry = previous.getCompound(i);
            final int slot = entry.getInt("Slot");
            if (slot < 0 || slot >= skins.length) continue;
            skins[slot] = entry.getBoolean("isSkinArmor");
        }
        
        final ListTag list = new ListTag();
        for (int slot = 0; slot < this.items.size(); slot++) {
            final ItemStack stack = this.items.get(slot).copyAndClear();
            if (stack.isEmpty() && !skins[slot]) continue;
            
            final CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", slot);
            if (skins[slot]) entry.putBoolean("isSkinArmor", true);
            list.add(stack.isEmpty() ? entry : stack.save(provider, entry));
        }
        
        stacks.put("Items", list);
        stacks.putInt("Size", this.items.size());
        data.put(CosmeticArmorHandler.KEY, stacks);
    }
    
    // ==================== OTHER ====================
    
    @Override
    public List<ItemStack> merge(CosmeticArmorContainer other) {
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
