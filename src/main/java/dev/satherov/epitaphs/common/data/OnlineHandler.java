package dev.satherov.epitaphs.common.data;

import lombok.experimental.UtilityClass;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.common.container.PlayerContainer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@UtilityClass
public final class OnlineHandler {
    
    ///
    /// Loads the backup for the given information and merges it with the given {@link ServerPlayer}.
    ///
    /// @param player Player to merge into.
    /// @param now    Timestamp of the backup.
    /// @param type   Type of backup.
    ///
    /// @return {@code 1} if the restore was successful, {@code 0} otherwise.
    ///
    /// @see OfflineHandler#restore(MinecraftServer, UUID, Instant, BackupType)
    ///
    public static int restore(ServerPlayer player, Instant now, BackupType type) {
        Path playerDirectory = DataHandler.getFileStorage(player.getServer()).resolve(player.getStringUUID());
        Path file = type.resolve(playerDirectory, now);
        
        try {
            CompoundTag backup = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            
            PlayerContainer playerContainer = PlayerContainer.create(player);
            PlayerContainer backupContainer = PlayerContainer.create(player.registryAccess(), backup);
            List<ItemStack> overflow = playerContainer.merge(backupContainer);
            // CHECK THIS BECAUSE WE DUPE CURIO
            List<ItemStack> dropped = playerContainer.inventory().insert(overflow);
            Epitaphs.log.debug("Merged {} with {} for {}", file.getFileName(), player.getUUID(), player.getGameProfile().getName());
            
            if (!dropped.isEmpty()) for (ItemStack stack : dropped) player.drop(stack, false);
            
            playerContainer.write(player);
            Epitaphs.log.debug("Restored {} with data from {}", player.getUUID(), file.getFileName());
            return 1;
            
        } catch (IOException e) {
            Epitaphs.log.error("Failed to restore {} with {}", player.getUUID(), file.getFileName(), e);
            return 0;
        }
    }
    
    ///
    /// Loads the backup for the given information and returns the items contained in it.
    ///
    /// @param player Player instance.
    /// @param now    Timestamp of the backup.
    /// @param type   Type of backup.
    ///
    /// @return list of items from the backup.
    ///
    /// @see OfflineHandler#gather(MinecraftServer, UUID, Instant, BackupType)
    ///
    public static List<ItemStack> gather(ServerPlayer player, Instant now, BackupType type) {
        Path storage = DataHandler.getFileStorage(player.getServer()).resolve(player.getStringUUID());
        Path file = type.resolve(storage, now);
        
        try {
            CompoundTag backup = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            PlayerContainer container = PlayerContainer.create(player.registryAccess(), backup);
            Epitaphs.log.debug("Loaded {} for {}", file.getFileName(), player.getStringUUID());
            return container.gather();
        } catch (IOException e) {
            Epitaphs.log.error("Failed to load {} for {}", file.getFileName(), player.getStringUUID(), e);
            return new ArrayList<>();
        }
    }
}
