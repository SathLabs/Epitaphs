package dev.satherov.epitaphs.common.container;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

public interface SaveContainer<T extends SaveContainer<T>> {
    
    void write(ServerPlayer player);
    
    void write(ValueInput input, ValueOutput output);
    
    List<ItemStack> merge(T other);
    
    List<ItemStack> gather();
    
    boolean isEmpty();
}
