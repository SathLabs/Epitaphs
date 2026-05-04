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
        
        try {
            Path file = type.resolve(playerDirectory, now);
            CompoundTag backup = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            
            PlayerContainer playerContainer = PlayerContainer.create(player);
            PlayerContainer backupContainer = PlayerContainer.create(player.registryAccess(), backup);
            List<ItemStack> overflow = playerContainer.merge(backupContainer);
            List<ItemStack> dropped = playerContainer.inventory().insert(overflow);
            Epitaphs.log.debug("Merged data from {} into {}", file.getFileName(), player.getGameProfile().getName());
            
            if (!dropped.isEmpty()) for (ItemStack stack : dropped) player.drop(stack, false);
            
            playerContainer.write(player);
            Epitaphs.log.debug("Restored data from {} for {}", file.getFileName(), player.getGameProfile().getName());
            return 1;
            
        } catch (IOException e) {
            Epitaphs.log.error("Failed to restore {} at {}", player.getUUID(), now.toString(), e);
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
        
        try {
            Path file = type.resolve(storage, now);
            
            CompoundTag backup = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            PlayerContainer container = PlayerContainer.create(player.registryAccess(), backup);
            Epitaphs.log.debug("Gathered data from {} for {}", file.getFileName(), player.getGameProfile().getName());
            return container.gather();
        } catch (IOException e) {
            Epitaphs.log.error("Failed to load {} at {}", now.toString(), player.getStringUUID(), e);
            return new ArrayList<>();
        }
    }
}
