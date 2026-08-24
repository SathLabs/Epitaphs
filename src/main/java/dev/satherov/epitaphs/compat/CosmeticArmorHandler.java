package dev.satherov.epitaphs.compat;

import lombok.experimental.UtilityClass;

import dev.satherov.epitaphs.Epitaphs;

import net.neoforged.fml.ModList;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

import lain.mods.cos.api.CosArmorAPI;
import lain.mods.cos.api.inventory.CAStacksBase;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

@UtilityClass
public class CosmeticArmorHandler {
    
    ///
    /// Key the cosmetic armor is stored under while it sits inside a player data tag.
    ///
    public static final String KEY = "epitaphs:cosmetic_armor";
    
    private static final String MOD_ID = "cosmeticarmorreworked";
    private static final String EXTENSION = ".cosarmor";
 
    public static boolean isLoaded() {
        return ModList.get().isLoaded(CosmeticArmorHandler.MOD_ID);
    }

    public static void clearAll(ServerPlayer player) {
        CAStacksBase stacks = CosArmorAPI.getCAStacks(player.getUUID());
        for (int slot = 0; slot < stacks.getSlots(); slot++) stacks.setStackInSlot(slot, ItemStack.EMPTY);
    }
    
    ///
    /// Copies the live cosmetic armor of the given player into a player data tag, so a backup taken from that tag holds it too.
    ///
    /// @param player Player to read from.
    /// @param data   Player data tag to write into.
    ///
    /// @see #load(MinecraftServer, UUID, CompoundTag)
    ///
    public static void save(ServerPlayer player, CompoundTag data) {
        CAStacksBase stacks = CosArmorAPI.getCAStacks(player.getUUID());
        data.put(CosmeticArmorHandler.KEY, stacks.serializeNBT(player.registryAccess()));
    }
    
    ///
    /// Copies the cosmetic armor file of an offline player into a player data tag, leaving the tag with nothing under
    /// [#KEY] when that player has no file yet.
    ///
    /// @param server Server instance.
    /// @param uuid   Player UUID.
    /// @param data   Player data tag to write into.
    ///
    /// @see #save(ServerPlayer, CompoundTag)
    /// @see #store(MinecraftServer, UUID, CompoundTag)
    ///
    public static void load(MinecraftServer server, UUID uuid, CompoundTag data) {
        data.remove(CosmeticArmorHandler.KEY);
        
        @Nullable CompoundTag stacks = CosmeticArmorHandler.read(server, uuid);
        if (stacks == null) return;
        data.put(CosmeticArmorHandler.KEY, stacks);
    }
    
    ///
    /// Takes the cosmetic armor back out of a player data tag and into the cosmetic armor file of an offline player.
    ///
    /// @param server Server instance.
    /// @param uuid   Player UUID.
    /// @param data   Player data tag to take the cosmetic armor out of.
    ///
    /// @see #load(MinecraftServer, UUID, CompoundTag)
    ///
    public static void store(MinecraftServer server, UUID uuid, CompoundTag data) {
        if (!data.contains(CosmeticArmorHandler.KEY, Tag.TAG_COMPOUND)) return;
        
        CompoundTag stacks = data.getCompound(CosmeticArmorHandler.KEY);
        data.remove(CosmeticArmorHandler.KEY);
        CosmeticArmorHandler.write(server, uuid, stacks);
    }
    
    ///
    /// Replaces the cosmetic armor of a player with the one held by the given player data tag and strips it back out of that tag.
    ///
    /// Unlike [#store(MinecraftServer, UUID, CompoundTag)] this discards whatever the player has, hidden armor flags included.
    ///
    /// @param server Server instance.
    /// @param uuid   Player UUID.
    /// @param data   Player data tag to take the cosmetic armor out of.
    ///
    public static void reset(MinecraftServer server, UUID uuid, CompoundTag data) {
        if (!data.contains(CosmeticArmorHandler.KEY, Tag.TAG_COMPOUND)) return;
        
        CompoundTag stacks = data.getCompound(CosmeticArmorHandler.KEY);
        data.remove(CosmeticArmorHandler.KEY);
        CosmeticArmorHandler.write(server, uuid, stacks);
        
        @Nullable ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player == null) return;
        CosArmorAPI.getCAStacks(uuid).deserializeNBT(player.registryAccess(), stacks);
    }
    
    ///
    /// Reads the cosmetic armor file of the given player.
    ///
    /// @param server Server instance.
    /// @param uuid   Player UUID.
    ///
    /// @return The stored cosmetic armor, or `null` if there is no file or it could not be read.
    ///
    private static @Nullable CompoundTag read(MinecraftServer server, UUID uuid) {
        Path file = CosmeticArmorHandler.file(server, uuid);
        
        try {
            return NbtIo.read(file);
        } catch (IOException e) {
            Epitaphs.log.error("Failed to read cosmetic armor for {} at {}", uuid, file.getFileName(), e);
            return null;
        }
    }
    
    ///
    /// Writes the cosmetic armor file of the given player.
    ///
    /// @param server Server instance.
    /// @param uuid   Player UUID.
    /// @param stacks Cosmetic armor to store.
    ///
    private static void write(MinecraftServer server, UUID uuid, CompoundTag stacks) {
        Path file = CosmeticArmorHandler.file(server, uuid);
        
        try {
            NbtIo.write(stacks, file);
            Epitaphs.log.debug("Wrote cosmetic armor for {} to {}", uuid, file.getFileName());
        } catch (IOException e) {
            Epitaphs.log.error("Failed to write cosmetic armor for {} at {}", uuid, file.getFileName(), e);
        }
    }
    
    ///
    /// Resolves the cosmetic armor file of the given player the same way CosmeticArmorReworked does.
    ///
    /// @param server Server instance.
    /// @param uuid   Player UUID.
    ///
    private static Path file(MinecraftServer server, UUID uuid) {
        return server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(uuid + CosmeticArmorHandler.EXTENSION);
    }
}
