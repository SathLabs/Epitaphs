package dev.satherov.epitaphs.common.data;

import lombok.experimental.UtilityClass;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.common.container.PlayerContainer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import com.mojang.serialization.DataResult;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@UtilityClass
public final class OfflineHandler {
    
    ///
    /// Loads the backup for the given information, merges it with the {@code Player data} and saves it back.
    ///
    /// @param server Server instance.
    /// @param uuid   Player UUID.
    /// @param now    Timestamp of the backup.
    /// @param type   Type of backup.
    ///
    /// @return {@code 1} if the restore was successful, {@code 0} otherwise.
    ///
    /// @see OnlineHandler#restore(ServerPlayer, Instant, BackupType)
    ///
    public static int restore(MinecraftServer server, UUID uuid, Instant now, BackupType type) {
        Path playerData = DataHandler.getPlayerDataStorage(server).resolve(uuid + ".dat");
        Path playerDirectory = DataHandler.getFileStorage(server).resolve(uuid.toString());
        Path backupFile = type.resolve(playerDirectory, now);
        
        try {
            RegistryAccess access = server.registryAccess();
            CompoundTag playerDataTag = NbtIo.readCompressed(playerData, NbtAccounter.unlimitedHeap());
            CompoundTag backupTag = NbtIo.readCompressed(backupFile, NbtAccounter.unlimitedHeap());
            
            final PlayerContainer playerContainer;
            try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(Epitaphs.log)) {
                playerContainer = PlayerContainer.create(TagValueInput.create(reporter, access, playerDataTag));
            }
            
            final PlayerContainer backupContainer;
            try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(Epitaphs.log)) {
                backupContainer = PlayerContainer.create(TagValueInput.create(reporter, access, backupTag));
            }
            
            List<ItemStack> overflow = playerContainer.merge(backupContainer);
            List<ItemStack> dropped = playerContainer.inventory().insert(overflow);
            Epitaphs.log.debug("Merged {} with {} for {}", backupFile.getFileName(), playerData.getFileName(), uuid);
            
            if (!dropped.isEmpty()) {
                Epitaphs.log.warn("Merging of playerdata for {} with backup at {} resulted in {} dropped items", uuid, backupFile.getFileName(), dropped.size());
                OfflineHandler.drop(server, playerDataTag, dropped);
            }
            
            try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(Epitaphs.log)) {
                TagValueOutput output = TagValueOutput.createWithContext(reporter, access);
                playerContainer.write(TagValueInput.create(reporter, access, playerDataTag), output);
                playerDataTag.merge(output.buildResult());
            }
            NbtIo.writeCompressed(playerDataTag, playerData);
            Epitaphs.log.debug("Wrote offline data from {} for {}", backupFile.getFileName(), uuid);
            return 1;
            
        } catch (IOException e) {
            Epitaphs.log.error("Failed to restore offline data at {} for {}", backupFile.getFileName(), uuid, e);
            return 0;
        }
    }
    
    ///
    /// Loads the backup for the given information and attempts to extract all items from within it.
    ///
    /// @param server Server instance.
    /// @param uuid   Player UUID.
    /// @param now    Timestamp of the backup.
    /// @param type   Type of backup.
    ///
    /// @return list of items from the backup.
    ///
    /// @see OnlineHandler#gather(ServerPlayer, Instant, BackupType)
    ///
    public static List<ItemStack> gather(MinecraftServer server, UUID uuid, Instant now, BackupType type) {
        Path playerDirectory = DataHandler.getFileStorage(server).resolve(uuid.toString());
        Path backupFile = type.resolve(playerDirectory, now);
        
        try {
            CompoundTag backup = NbtIo.readCompressed(backupFile, NbtAccounter.unlimitedHeap());
            final PlayerContainer container;
            try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(Epitaphs.log)) {
                container = PlayerContainer.create(TagValueInput.create(reporter, server.registryAccess(), backup));
            }
            Epitaphs.log.debug("Loaded {} for {}", backupFile.getFileName(), uuid);
            return container.gather();
        } catch (IOException e) {
            Epitaphs.log.error("Failed to load {} for {}", backupFile.getFileName(), uuid, e);
            return new ArrayList<>();
        }
    }
    
    private static void drop(MinecraftServer server, CompoundTag data, List<ItemStack> items) {
        DataResult<ResourceKey<Level>> dimensionResult = Level.RESOURCE_KEY_CODEC.parse(NbtOps.INSTANCE, data);
        DataResult<BlockPos> posResult = BlockPos.CODEC.parse(NbtOps.INSTANCE, data);
        if (dimensionResult.isError() || posResult.isError()) {
            Epitaphs.log.error("Failed to drop items due to invalid data");
            return;
        }
        
        ResourceKey<Level> dimension = dimensionResult.getOrThrow();
        BlockPos pos = posResult.getOrThrow();
        ServerLevel level = server.getLevel(dimension);
        if (level == null) {
            Epitaphs.log.warn("Failed to drop items due to invalid dimension '{}'", dimension.identifier());
            return;
        }
        
        for (ItemStack item : items) Block.popResourceFromFace(level, pos, Direction.UP, item);
        Epitaphs.log.info("Dropped {} items at {} in {}", items.size(), pos, dimension.identifier());
    }
}
