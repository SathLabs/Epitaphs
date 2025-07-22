package dev.satherov.epitaphs.common.component;

import net.neoforged.neoforge.common.util.INBTSerializable;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.UnknownNullability;

public class EPSoulboundAttachment implements INBTSerializable<CompoundTag> {

    NonNullList<ItemStack> items = NonNullList.create();
    NonNullList<ItemStack> armor = NonNullList.create();
    NonNullList<ItemStack> offhand = NonNullList.create();
    NonNullList<ItemStack> curio = NonNullList.create();
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

    public NonNullList<ItemStack> getCurio() {
        return curio;
    }

    public int getExperience() {
        return experience;
    }

    public void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    public void setArmor(NonNullList<ItemStack> armor) {
        this.armor = armor;
    }

    public void setOffhand(NonNullList<ItemStack> offhand) {
        this.offhand = offhand;
    }

    public void setCurio(NonNullList<ItemStack> curio) {
        this.curio = curio;
    }

    public void setExperience(int experience) {
        this.experience = experience;
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
