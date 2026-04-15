package dev.satherov.epitaphs.common.container;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface SaveContainer<T extends SaveContainer<T>> {
    
    void write(ServerPlayer player);
    
    CompoundTag write(HolderLookup.Provider provider, CompoundTag data);
    
    List<ItemStack> merge(T other);
    
    List<ItemStack> gather();
    
    boolean isEmpty();
}
