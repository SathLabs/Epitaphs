package dev.satherov.epitaphs.common.container;

import dev.satherov.epitaphs.compat.CuriosHandler;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record PlayerContainer(UUID uuid, InventoryContainer inventory, CuriosContainer curios) implements SaveContainer<PlayerContainer> {
    
    public static final Codec<PlayerContainer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("uuid").forGetter(PlayerContainer::uuid),
            InventoryContainer.CODEC.fieldOf("inventory").forGetter(PlayerContainer::inventory),
            CuriosContainer.CODEC.fieldOf("curios").forGetter(PlayerContainer::curios)
    ).apply(instance, PlayerContainer::new));
    
    public static PlayerContainer empty() {
        return new PlayerContainer(UUID.nameUUIDFromBytes(new byte[0]), InventoryContainer.empty(), CuriosContainer.empty());
    }
    
    public static PlayerContainer create(ServerPlayer player) {
        final UUID uuid = player.getUUID();
        final InventoryContainer inventory = InventoryContainer.create(player);
        final CuriosContainer curios = CuriosHandler.isLoaded() ? CuriosContainer.create(player) : CuriosContainer.empty();
        return new PlayerContainer(uuid, inventory, curios);
    }
    
    public static PlayerContainer create(HolderLookup.Provider provider, CompoundTag data) {
        final UUID uuid = data.getUUID("UUID");
        final InventoryContainer inventory = InventoryContainer.create(provider, data);
        final CuriosContainer curios = CuriosHandler.isLoaded() ? CuriosContainer.create(provider, data) : CuriosContainer.empty();
        return new PlayerContainer(uuid, inventory, curios);
    }
    
    @Override
    public void write(ServerPlayer player) {
        this.inventory.write(player);
        if (CuriosHandler.isLoaded()) this.curios.write(player);
    }
    
    @Override
    public CompoundTag write(HolderLookup.Provider provider, CompoundTag data) {
        this.inventory.write(provider, data);
        if (CuriosHandler.isLoaded()) this.curios.write(provider, data);
        return data;
    }
    
    @Override
    public List<ItemStack> merge(PlayerContainer other) {
        final List<ItemStack> result = new ArrayList<>();
        result.addAll(this.inventory.merge(other.inventory));
        if (CuriosHandler.isLoaded()) result.addAll(this.curios.merge(other.curios));
        return result;
    }
    
    @Override
    public List<ItemStack> gather() {
        final List<ItemStack> result = new ArrayList<>();
        result.addAll(this.inventory.gather());
        if (CuriosHandler.isLoaded()) result.addAll(this.curios.gather());
        return result;
    }
    
    @Override
    public boolean isEmpty() {
        return this.inventory.isEmpty() && this.curios.isEmpty();
    }
}
