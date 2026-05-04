package dev.satherov.epitaphs.common.container;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.compat.CuriosHandler;

import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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
        Epitaphs.log.debug("Created PlayerContainer for {} (live)", player.getGameProfile().name());
        return new PlayerContainer(uuid, inventory, curios);
    }
    
    public static PlayerContainer create(ValueInput input) {
        final UUID uuid = input.read("UUID", UUIDUtil.CODEC).orElse(UUID.nameUUIDFromBytes(new byte[0]));
        final InventoryContainer inventory = InventoryContainer.create(input);
        final CuriosContainer curios = CuriosHandler.isLoaded() ? CuriosContainer.create(input) : CuriosContainer.empty();
        Epitaphs.log.debug("Created PlayerContainer for {} (offline)", uuid);
        return new PlayerContainer(uuid, inventory, curios);
    }
    
    @Override
    public void write(ServerPlayer player) {
        this.inventory.write(player);
        if (CuriosHandler.isLoaded()) this.curios.write(player);
        Epitaphs.log.debug("Wrote PlayerContainer for {} (online)", player.getGameProfile().name());
    }
    
    @Override
    public void write(ValueInput input, ValueOutput output) {
        this.inventory.write(input, output);
        if (CuriosHandler.isLoaded()) this.curios.write(input, output);
        final UUID uuid = input.read("UUID", UUIDUtil.CODEC).orElse(UUID.nameUUIDFromBytes(new byte[0]));
        Epitaphs.log.debug("Wrote PlayerContainer for {} (offline)", uuid);
    }
    
    @Override
    public List<ItemStack> merge(PlayerContainer other) {
        final List<ItemStack> result = new ArrayList<>(this.inventory.merge(other.inventory));
        if (CuriosHandler.isLoaded()) result.addAll(this.curios.merge(other.curios));
        Epitaphs.log.debug("Merged PlayerContainer for {}", this.uuid);
        return result;
    }
    
    @Override
    public List<ItemStack> gather() {
        final List<ItemStack> result = new ArrayList<>(this.inventory.gather());
        if (CuriosHandler.isLoaded()) result.addAll(this.curios.gather());
        Epitaphs.log.debug("Gathered PlayerContainer for {}", this.uuid);
        return result;
    }
    
    @Override
    public boolean isEmpty() {
        return this.inventory.isEmpty() && this.curios.isEmpty();
    }
}
