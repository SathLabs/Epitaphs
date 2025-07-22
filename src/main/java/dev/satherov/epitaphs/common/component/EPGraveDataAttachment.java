package dev.satherov.epitaphs.common.component;

import dev.satherov.epitaphs.Epitaphs;

import net.neoforged.neoforge.common.util.INBTSerializable;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.UnknownNullability;

import java.util.ArrayList;
import java.util.List;

public class EPGraveDataAttachment implements INBTSerializable<CompoundTag> {

    String timestamp = "";
    String owner = "";
    List<ItemStack> additional = new ArrayList<>();

    public EPGraveDataAttachment create(ServerPlayer player, String timestamp) {
        this.timestamp = timestamp;
        this.owner = player.getStringUUID();
        return this;
    }

    public void saveAdditional(List<ItemStack> additional) {
        this.additional = additional;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getOwner() {
        return owner;
    }

    public List<ItemStack> getAdditional() {
        return additional;
    }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag data = new CompoundTag();
        data.putString("uuid", owner);
        data.putString("timestamp", timestamp);

        ListTag stacks = new ListTag();
        for (ItemStack stack : additional) {
            if (stack.isEmpty()) continue;
            stacks.add(stack.save(provider, new CompoundTag()));
        }
        if (!stacks.isEmpty()) data.put("additional", stacks);

        return data;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        owner = nbt.getString("uuid");
        timestamp = nbt.getString("timestamp");

        List<ItemStack> additional = new ArrayList<>();
        if (nbt.contains("additional")) {
            ListTag stacks = nbt.getList("additional", Tag.TAG_COMPOUND);
            for (int i = 0; i < stacks.size(); i++) {
                CompoundTag tag = stacks.getCompound(i);
                ItemStack stack = ItemStack.parseOptional(provider, tag);
                if (stack.isEmpty()) continue;
                additional.add(stack);
            }
        }
        this.additional = additional;
    }
}
