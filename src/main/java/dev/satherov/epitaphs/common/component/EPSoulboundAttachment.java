package dev.satherov.epitaphs.common.component;

import dev.satherov.epitaphs.Epitaphs;

import net.neoforged.neoforge.common.util.INBTSerializable;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.UnknownNullability;

import java.util.ArrayList;
import java.util.List;

public class EPSoulboundAttachment implements INBTSerializable<CompoundTag> {
    
    NonNullList<ItemStack> items = NonNullList.create();
    NonNullList<ItemStack> armor = NonNullList.create();
    NonNullList<ItemStack> offhand = NonNullList.create();
    List<ItemStack> curio = new ArrayList<>();
    List<ItemStack> cosmetics = new ArrayList<>();
    int experience = 0;
    
    public NonNullList<ItemStack> getItems() {
        return items;
    }
    
    public NonNullList<ItemStack> getArmor() {
        return armor;
    }
    
    public NonNullList<ItemStack> getOffhand() {
        return offhand;
    }
    
    public List<ItemStack> getCurio() {
        return curio;
    }
    
    public List<ItemStack> getCosmetics() {
        return cosmetics;
    }
    
    public int getExperience() {
        return experience;
    }
    
    public void setItems(ServerPlayer player, NonNullList<ItemStack> items) {
        if (this.items.isEmpty() || this.items.size() != items.size()) {
            this.items = items;
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            ItemStack present = this.items.get(i);
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;
            if (present.isEmpty()) {
                this.items.set(i, stack);
            } else {
                Epitaphs.LOGGER.warn("Soulbound item {} could not be saved to inventory because data was already occupied with {}. Did some mod kill us twice?", stack, present);
                player.getInventory().add(stack);
            }
        }
    }
    
    public void setArmor(ServerPlayer player, NonNullList<ItemStack> armor) {
        if (this.armor.isEmpty() || this.armor.size() != armor.size()) {
            this.armor = armor;
            return;
        }
        for (int i = 0; i < armor.size(); i++) {
            ItemStack present = this.armor.get(i);
            ItemStack stack = armor.get(i);
            if (stack.isEmpty()) continue;
            if (present.isEmpty()) {
                this.armor.set(i, stack);
            } else {
                Epitaphs.LOGGER.warn("Soulbound item {} could not be saved to armor because data was already occupied with {}. Did some mod kill us twice?", stack, present);
                player.getInventory().add(stack);
            }
        }
    }
    
    public void setOffhand(ServerPlayer player, NonNullList<ItemStack> offhand) {
        if (this.offhand.isEmpty() || this.offhand.size() != offhand.size()) {
            this.offhand = offhand;
            return;
        }
        for (int i = 0; i < offhand.size(); i++) {
            if (i >= this.offhand.size()) {
                this.offhand.add(ItemStack.EMPTY);
            }
            ItemStack present = this.offhand.get(i);
            ItemStack stack = offhand.get(i);
            if (stack.isEmpty()) continue;
            if (present.isEmpty()) {
                this.offhand.set(i, stack);
            } else {
                Epitaphs.LOGGER.warn("Soulbound item {} could not be saved to offhand because data was already occupied with {}. Did some mod kill us twice?", stack, present);
                player.getInventory().add(stack);
            }
        }
    }
    
    public void setCurio(ServerPlayer player, List<ItemStack> curio, List<ItemStack> cosmetics) {
        if (this.curio.isEmpty() || this.curio.size() != curio.size()) {
            this.curio = curio;
        } else {
            for (int i = 0; i < curio.size(); i++) {
                ItemStack present = this.curio.get(i);
                ItemStack stack = curio.get(i);
                if (stack.isEmpty()) continue;
                if (present.isEmpty()) {
                    this.curio.set(i, stack);
                } else {
                    Epitaphs.LOGGER.warn("Soulbound item {} could not be saved to curio because data was already occupied with {}. Did some mod kill us twice?", stack, present);
                    player.getInventory().add(stack);
                }
            }
        }
        
        if (this.cosmetics.isEmpty() || this.cosmetics.size() != cosmetics.size()) {
            this.cosmetics = cosmetics;
        } else {
            for (int i = 0; i < cosmetics.size(); i++) {
                ItemStack present = this.cosmetics.get(i);
                ItemStack stack = cosmetics.get(i);
                if (stack.isEmpty()) continue;
                if (present.isEmpty()) {
                    this.cosmetics.set(i, stack);
                } else {
                    Epitaphs.LOGGER.warn("Soulbound item {} could not be saved to cosmetics because data was already occupied with {}. Did some mod kill us twice?", stack, present);
                    player.getInventory().add(stack);
                }
            }
        }
    }
    
    public void setExperience(int experience) {
        this.experience = experience;
    }
    
    public boolean isEmpty() {
        return items.isEmpty() && armor.isEmpty() && offhand.isEmpty() && curio.isEmpty();
    }
    
    public void clear() {
        items.clear();
        armor.clear();
        offhand.clear();
        curio.clear();
        experience = 0;
    }
    
    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag data = new CompoundTag();
        
        CompoundTag itemsTag = new CompoundTag();
        ListTag itemsList = new ListTag();
        for (int i = 0; i < this.items.size(); i++) {
            if (!this.items.get(i).isEmpty()) {
                CompoundTag compoundtag = new CompoundTag();
                compoundtag.putByte("Slot", (byte) i);
                itemsList.add(this.items.get(i).save(provider, compoundtag));
            }
        }
        itemsTag.put("Items", itemsList);
        itemsTag.putInt("Count", this.items.size());
        data.put("items", itemsTag);
        
        CompoundTag armorTag = new CompoundTag();
        ListTag armorList = new ListTag();
        for (int j = 0; j < this.armor.size(); j++) {
            if (!this.armor.get(j).isEmpty()) {
                CompoundTag compoundtag1 = new CompoundTag();
                compoundtag1.putByte("Slot", (byte) j);
                armorList.add(this.armor.get(j).save(provider, compoundtag1));
            }
        }
        armorTag.put("Items", armorList);
        armorTag.putInt("Count", this.armor.size());
        data.put("armor", armorTag);
        
        CompoundTag offhandTag = new CompoundTag();
        ListTag offhandList = new ListTag();
        for (int k = 0; k < this.offhand.size(); k++) {
            if (!this.offhand.get(k).isEmpty()) {
                CompoundTag compoundtag2 = new CompoundTag();
                compoundtag2.putByte("Slot", (byte) k);
                offhandList.add(this.offhand.get(k).save(provider, compoundtag2));
            }
        }
        offhandTag.put("Items", offhandList);
        offhandTag.putInt("Count", this.offhand.size());
        data.put("offhand", offhandTag);
        
        CompoundTag curioTag = new CompoundTag();
        ListTag curioList = new ListTag();
        for (int l = 0; l < this.curio.size(); l++) {
            if (!this.curio.get(l).isEmpty()) {
                CompoundTag compoundtag3 = new CompoundTag();
                compoundtag3.putByte("Slot", (byte) l);
                curioList.add(this.curio.get(l).save(provider, compoundtag3));
            }
        }
        curioTag.put("Items", curioList);
        curioTag.putInt("Count", this.curio.size());
        data.put("curio", curioTag);
        
        if (experience > 0) data.putInt("experience", experience);
        return data;
    }
    
    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        if (nbt.contains("items")) {
            CompoundTag itemsTag = nbt.getCompound("items");
            ListTag items = itemsTag.getList("Items", Tag.TAG_COMPOUND);
            int count = itemsTag.getInt("Count");
            
            NonNullList<ItemStack> itemStacks = NonNullList.withSize(count, ItemStack.EMPTY);
            for (int i = 0; i < items.size(); i++) {
                CompoundTag tag = items.getCompound(i);
                int slot = tag.getByte("Slot") & 255;
                ItemStack stack = ItemStack.parseOptional(provider, tag);
                itemStacks.set(slot, stack);
            }
            this.items = itemStacks;
        }
        if (nbt.contains("armor")) {
            CompoundTag armorTag = nbt.getCompound("armor");
            ListTag armor = armorTag.getList("Items", Tag.TAG_COMPOUND);
            int count = armorTag.getInt("Count");
            
            NonNullList<ItemStack> armorStacks = NonNullList.withSize(count, ItemStack.EMPTY);
            for (int i = 0; i < armor.size(); i++) {
                CompoundTag tag = armor.getCompound(i);
                int slot = tag.getByte("Slot") & 255;
                ItemStack stack = ItemStack.parseOptional(provider, tag);
                armorStacks.set(slot, stack);
            }
            this.armor = armorStacks;
        }
        if (nbt.contains("offhand")) {
            CompoundTag offhandTag = nbt.getCompound("offhand");
            ListTag offhand = offhandTag.getList("Items", Tag.TAG_COMPOUND);
            int count = offhandTag.getInt("Count");
            
            NonNullList<ItemStack> offhandStacks = NonNullList.withSize(count, ItemStack.EMPTY);
            for (int i = 0; i < offhand.size(); i++) {
                CompoundTag tag = offhand.getCompound(i);
                int slot = tag.getByte("Slot") & 255;
                ItemStack stack = ItemStack.parseOptional(provider, tag);
                offhandStacks.set(slot, stack);
            }
            this.offhand = offhandStacks;
        }
        if (nbt.contains("curio")) {
            CompoundTag curioTag = nbt.getCompound("curio");
            ListTag curio = curioTag.getList("Items", Tag.TAG_COMPOUND);
            int count = curioTag.getInt("Count");
            
            NonNullList<ItemStack> curioStacks = NonNullList.withSize(count, ItemStack.EMPTY);
            for (int i = 0; i < curio.size(); i++) {
                CompoundTag tag = curio.getCompound(i);
                int slot = tag.getByte("Slot") & 255;
                ItemStack stack = ItemStack.parseOptional(provider, tag);
                curioStacks.set(slot, stack);
            }
            this.curio = curioStacks;
        }
        if (nbt.contains("experience")) {
            this.experience = nbt.getInt("experience");
        }
    }
}
