package dev.satherov.epitaphs.common.container;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.compat.AccessoriesHandler;
import dev.satherov.epitaphs.compat.CosmeticArmorHandler;
import dev.satherov.epitaphs.compat.CuriosHandler;
import dev.satherov.epitaphs.compat.ToolbeltHandler;

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
public record PlayerContainer(
        UUID uuid,
        InventoryContainer inventory,
        CuriosContainer curios,
        AccessoriesContainer accessories,
        CosmeticArmorContainer cosmeticArmor,
        ToolBeltContainer toolbelt
) implements SaveContainer<PlayerContainer> {
    
    public static final Codec<PlayerContainer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("uuid").forGetter(PlayerContainer::uuid),
            InventoryContainer.CODEC.fieldOf("inventory").forGetter(PlayerContainer::inventory),
            CuriosContainer.CODEC.optionalFieldOf("curios", CuriosContainer.empty()).forGetter(PlayerContainer::curios),
            AccessoriesContainer.CODEC.optionalFieldOf("accessories", AccessoriesContainer.empty()).forGetter(PlayerContainer::accessories),
            CosmeticArmorContainer.CODEC.optionalFieldOf("cosmetic_armor", CosmeticArmorContainer.empty()).forGetter(PlayerContainer::cosmeticArmor),
            ToolBeltContainer.CODEC.optionalFieldOf("toolbelt", ToolBeltContainer.empty()).forGetter(PlayerContainer::toolbelt)
    ).apply(instance, PlayerContainer::new));
    
    public static PlayerContainer empty() {
        return new PlayerContainer(UUID.nameUUIDFromBytes(new byte[0]), InventoryContainer.empty(), CuriosContainer.empty(), AccessoriesContainer.empty(), CosmeticArmorContainer.empty(), ToolBeltContainer.empty());
    }
    
    public static PlayerContainer create(ServerPlayer player) {
        final UUID uuid = player.getUUID();
        final InventoryContainer inventory = InventoryContainer.create(player);
        final CuriosContainer curios = CuriosHandler.isLoaded() ? CuriosContainer.create(player) : CuriosContainer.empty();
        final AccessoriesContainer accessories = AccessoriesHandler.isLoaded() ? AccessoriesContainer.create(player) : AccessoriesContainer.empty();
        final CosmeticArmorContainer cosmeticArmor = CosmeticArmorHandler.isLoaded() ? CosmeticArmorContainer.create(player) : CosmeticArmorContainer.empty();
        final ToolBeltContainer toolbelt = ToolbeltHandler.isLoaded() ? ToolBeltContainer.create(player) : ToolBeltContainer.empty();
        Epitaphs.log.debug("Created PlayerContainer for {} (live)", player.getGameProfile().getName());
        return new PlayerContainer(uuid, inventory, curios, accessories, cosmeticArmor, toolbelt);
    }
    
    public static PlayerContainer create(HolderLookup.Provider provider, CompoundTag data) {
        final UUID uuid = data.getUUID("UUID");
        final InventoryContainer inventory = InventoryContainer.create(provider, data);
        final CuriosContainer curios = CuriosHandler.isLoaded() ? CuriosContainer.create(provider, data) : CuriosContainer.empty();
        final AccessoriesContainer accessories = AccessoriesHandler.isLoaded() ? AccessoriesContainer.create(provider, data) : AccessoriesContainer.empty();
        final CosmeticArmorContainer cosmeticArmor = CosmeticArmorHandler.isLoaded() ? CosmeticArmorContainer.create(provider, data) : CosmeticArmorContainer.empty();
        final ToolBeltContainer toolbelt = ToolbeltHandler.isLoaded() ? ToolBeltContainer.create(provider, data) : ToolBeltContainer.empty();
        Epitaphs.log.debug("Created PlayerContainer for {} (offline)", uuid);
        return new PlayerContainer(uuid, inventory, curios, accessories, cosmeticArmor, toolbelt);
    }
    
    @Override
    public void write(ServerPlayer player) {
        this.inventory.write(player);
        if (CuriosHandler.isLoaded()) this.curios.write(player);
        if (AccessoriesHandler.isLoaded()) this.accessories.write(player);
        if (CosmeticArmorHandler.isLoaded()) this.cosmeticArmor.write(player);
        if (ToolbeltHandler.isLoaded()) this.toolbelt.write(player);
        Epitaphs.log.debug("Wrote PlayerContainer for {} (online)", player.getGameProfile().getName());
    }
    
    @Override
    public void write(HolderLookup.Provider provider, CompoundTag data) {
        this.inventory.write(provider, data);
        if (CuriosHandler.isLoaded()) this.curios.write(provider, data);
        if (AccessoriesHandler.isLoaded()) this.accessories.write(provider, data);
        if (CosmeticArmorHandler.isLoaded()) this.cosmeticArmor.write(provider, data);
        if (ToolbeltHandler.isLoaded()) this.toolbelt.write(provider, data);
        Epitaphs.log.debug("Wrote PlayerContainer for {} (offline)", data.getUUID("UUID"));
    }
    
    @Override
    public List<ItemStack> merge(PlayerContainer other) {
        final List<ItemStack> result = new ArrayList<>(this.inventory.merge(other.inventory));
        if (CuriosHandler.isLoaded()) result.addAll(this.curios.merge(other.curios));
        if (AccessoriesHandler.isLoaded()) result.addAll(this.accessories.merge(other.accessories));
        if (CosmeticArmorHandler.isLoaded()) result.addAll(this.cosmeticArmor.merge(other.cosmeticArmor));
        if (ToolbeltHandler.isLoaded()) result.addAll(this.toolbelt.merge(other.toolbelt));
        Epitaphs.log.debug("Merged PlayerContainer for {}", this.uuid);
        return result;
    }
    
    @Override
    public List<ItemStack> gather() {
        final List<ItemStack> result = new ArrayList<>(this.inventory.gather());
        if (CuriosHandler.isLoaded()) result.addAll(this.curios.gather());
        if (AccessoriesHandler.isLoaded()) result.addAll(this.accessories.gather());
        if (CosmeticArmorHandler.isLoaded()) result.addAll(this.cosmeticArmor.gather());
        if (ToolbeltHandler.isLoaded()) result.addAll(this.toolbelt.gather());
        Epitaphs.log.debug("Gathered PlayerContainer for {}", this.uuid);
        return result;
    }
    
    @Override
    public boolean isEmpty() {
        return this.inventory.isEmpty() && this.curios.isEmpty() && this.accessories.isEmpty() && this.cosmeticArmor.isEmpty() && this.toolbelt.isEmpty();
    }
}
