package dev.satherov.epitaphs.common.container;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.compat.AccessoriesHandler;
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

@SuppressWarnings("LoggingSimilarMessage")
public record PlayerContainer(UUID uuid, InventoryContainer inventory, CuriosContainer curios, AccessoriesContainer accessories) implements SaveContainer<PlayerContainer> {
    
    public static final Codec<PlayerContainer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("uuid").forGetter(PlayerContainer::uuid),
            InventoryContainer.CODEC.fieldOf("inventory").forGetter(PlayerContainer::inventory),
            CuriosContainer.CODEC.fieldOf("curios").forGetter(PlayerContainer::curios),
            AccessoriesContainer.CODEC.fieldOf("accessories").forGetter(PlayerContainer::accessories)
    ).apply(instance, PlayerContainer::new));
    
    public static PlayerContainer empty() {
        return new PlayerContainer(UUID.nameUUIDFromBytes(new byte[0]), InventoryContainer.empty(), CuriosContainer.empty(), AccessoriesContainer.empty());
    }
    
    public static PlayerContainer create(ServerPlayer player) {
        final UUID uuid = player.getUUID();
        final InventoryContainer inventory = InventoryContainer.create(player);
        final CuriosContainer curios = CuriosHandler.isLoaded() ? CuriosContainer.create(player) : CuriosContainer.empty();
        final AccessoriesContainer accessories = AccessoriesHandler.isLoaded() ? AccessoriesContainer.create(player) : AccessoriesContainer.empty();
        Epitaphs.log.debug("Created PlayerContainer for {} (live)", player.getGameProfile().getName());
        return new PlayerContainer(uuid, inventory, curios, accessories);
    }
    
    public static PlayerContainer create(HolderLookup.Provider provider, CompoundTag data) {
        final UUID uuid = data.getUUID("UUID");
        final InventoryContainer inventory = InventoryContainer.create(provider, data);
        final CuriosContainer curios = CuriosHandler.isLoaded() ? CuriosContainer.create(provider, data) : CuriosContainer.empty();
        final AccessoriesContainer accessories = AccessoriesHandler.isLoaded() ? AccessoriesContainer.create(provider, data) : AccessoriesContainer.empty();
        Epitaphs.log.debug("Created PlayerContainer for {} (offline)", uuid);
        return new PlayerContainer(uuid, inventory, curios, accessories);
    }
    
    @Override
    public void write(ServerPlayer player) {
        this.inventory.write(player);
        if (CuriosHandler.isLoaded()) this.curios.write(player);
        if (AccessoriesHandler.isLoaded()) this.accessories.write(player);
        Epitaphs.log.debug("Wrote PlayerContainer for {} (online)", player.getGameProfile().getName());
    }
    
    @Override
    public void write(HolderLookup.Provider provider, CompoundTag data) {
        this.inventory.write(provider, data);
        if (CuriosHandler.isLoaded()) this.curios.write(provider, data);
        if (AccessoriesHandler.isLoaded()) this.accessories.write(provider, data);
        Epitaphs.log.debug("Wrote PlayerContainer for {} (offline)", data.getUUID("UUID"));
    }
    
    @Override
    public List<ItemStack> merge(PlayerContainer other) {
        final List<ItemStack> result = new ArrayList<>();
        result.addAll(this.inventory.merge(other.inventory));
        if (CuriosHandler.isLoaded()) result.addAll(this.curios.merge(other.curios));
        if (AccessoriesHandler.isLoaded()) result.addAll(this.accessories.merge(other.accessories));
        Epitaphs.log.debug("Merged PlayerContainer for {}", this.uuid);
        return result;
    }
    
    @Override
    public List<ItemStack> gather() {
        final List<ItemStack> result = new ArrayList<>();
        result.addAll(this.inventory.gather());
        if (CuriosHandler.isLoaded()) result.addAll(this.curios.gather());
        if (AccessoriesHandler.isLoaded()) result.addAll(this.accessories.gather());
        Epitaphs.log.debug("Gathered PlayerContainer for {}", this.uuid);
        return result;
    }
    
    @Override
    public boolean isEmpty() {
        return this.inventory.isEmpty() && this.curios.isEmpty() && this.accessories.isEmpty();
    }
}
