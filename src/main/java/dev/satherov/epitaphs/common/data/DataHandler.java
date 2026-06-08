package dev.satherov.epitaphs.common.data;

import lombok.experimental.UtilityClass;

import dev.satherov.epitaphs.EPConfig;
import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.common.container.PlayerContainer;
import dev.satherov.sathlib.util.SLStringUtils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
public class DataHandler {
    
    private static final String LEAP_YEAR_PATTERN = "(?:(?:[02468][048]|[13579][26])00|\\d{2}(?:0[48]|[2468][048]|[13579][26]))";
    private static final String DATE_STAMP_PATTERN = "(?:" + DataHandler.LEAP_YEAR_PATTERN + "-02-29|\\d{4}-(?:(?:0[13578]|1[02])-(?:0[1-9]|[12]\\d|3[01])|(?:0[469]|11)-(?:0[1-9]|[12]\\d|30)|02-(?:0[1-9]|1\\d|2[0-8])))";
    private static final String TIME_STAMP_PATTERN = "(?:[01]\\d|2[0-3])-[0-5]\\d-[0-5]\\d";
    
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm-ss").withResolverStyle(ResolverStyle.STRICT).withZone(ZoneOffset.UTC);
    public static final Pattern DATE_PATTERN = Pattern.compile("^" + DataHandler.DATE_STAMP_PATTERN + "-" + DataHandler.TIME_STAMP_PATTERN + "(?=$|-)");
    private static final Locale SYS_LOCALE = Locale.getDefault(Locale.Category.FORMAT);
    private static final String DATE_FORMATTER = DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.SHORT, null, IsoChronology.INSTANCE, DataHandler.SYS_LOCALE);
    public static final DateTimeFormatter ISO8601_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);
    public static final DateTimeFormatter SYSTEM_FORMATTER = DateTimeFormatter.ofPattern(DataHandler.DATE_FORMATTER + " HH:mm:ss", DataHandler.SYS_LOCALE).withZone(ZoneId.systemDefault());

    ///
    /// Gets the file path to the world folder.
    ///
    /// @param server The server instance.
    ///
    /// @return The world folder path.
    ///
    private static Path world(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT)
                .normalize()
                .toAbsolutePath();
    }
    
    ///
    /// Gets the file path to the player data folder.
    ///
    /// @param server The server instance.
    ///
    /// @return The player data folder path.
    ///
    public static Path getPlayerDataStorage(MinecraftServer server) {
        return DataHandler.world(server)
                .resolve("players")
                .resolve("data");
    }
    
    ///
    /// Gets the file path to the storage directory.
    ///
    /// @param server The server instance.
    ///
    /// @return The storage directory path.
    ///
    public static Path getFileStorage(MinecraftServer server) {
        return DataHandler.world(server)
                .resolve("data")
                .resolve(Epitaphs.MOD_ID);
    }
    
    
    ///
    /// Purges old backup files from the storage directory.
    ///
    /// @param player  The player UUID who owns these backups.
    /// @param storage The storage directory.
    /// @param type    The type of backup to purge.
    ///
    private static void purge(UUID player, Path storage, BackupType type) {
        final String name = SLStringUtils.lower(type.name());
        try (Stream<Path> stream = Files.walk(storage, 1)) {
            List<Path> files = new ArrayList<>(
                    stream.filter(Files::isRegularFile)
                            .filter(p -> type.purgePattern().matcher(p.getFileName().toString()).matches())
                            .sorted(Comparator.naturalOrder())
                            .toList()
            );
            while (type.limit() > 0 && files.size() > type.limit()) {
                Path oldest = files.removeFirst();
                Files.deleteIfExists(oldest);
                Epitaphs.log.debug("Removed old {} file {}", name, oldest.getFileName());
            }
            
            final int backupMaxDaysAge = EPConfig.Server.getBackupMaxAgeDays();
            if (backupMaxDaysAge <= 0) return;
            
            final Instant cutoff = Instant.now().minus(backupMaxDaysAge, ChronoUnit.DAYS);
            DataHandler.purgeByTimestamp(player, files, cutoff, backupMaxDaysAge);
            
        } catch (IOException e) {
            Epitaphs.log.error("Failed to remove old files for '{}' at {}", name, storage.getFileName(), e);
        }
    }
    
    ///
    /// Purges backups older than the configured backup age.
    ///
    /// @param player  The player UUID who owns these backups.
    /// @param files   The list of backup files.
    /// @param cutoff  The cutoff timestamp.
    /// @param allowed The maximum age of the backup in days.
    ///
    private static void purgeByTimestamp(UUID player, List<Path> files, Instant cutoff, int allowed) throws IOException {
        for (final Path file : files) {
            final String filename = file.getFileName().toString();
            final @Nullable Instant timestamp = DataHandler.parseTimestamp(filename);
            if (timestamp == null) continue;
            if (!timestamp.isBefore(cutoff)) continue;
            
            Files.deleteIfExists(file);
            
            Epitaphs.log.debug(
                    "Removed file {} owned by {} since it was older than {} days (Created at {})",
                    filename,
                    player,
                    allowed,
                    timestamp
            );
        }
    }
    
    private static @Nullable Instant parseTimestamp(String filename) {
        final Matcher matcher = DataHandler.DATE_PATTERN.matcher(filename);
        if (!matcher.find()) return null;
        
        try {
            return Instant.from(DataHandler.FORMATTER.parse(matcher.group()));
        } catch (DateTimeException e) {
            Epitaphs.log.warn("Skipping backup file {} with invalid timestamp {}", filename, matcher.group(), e);
            return null;
        }
    }
    
    
    ///
    /// Returns a list of all players with saved data.
    ///
    /// @param server The server instance.
    ///
    /// @return list of player UUIDs
    ///
    public static LinkedList<String> listPlayer(MinecraftServer server) {
        final Path storage = DataHandler.getFileStorage(server);
        if (!Files.exists(storage) || !Files.isDirectory(storage)) {
            Epitaphs.log.debug("No data directory found at {}", storage.getFileName());
            return new LinkedList<>();
        }
        
        try (Stream<Path> stream = Files.walk(storage, 1)) {
            return stream.filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(path -> {
                        try {
                            UUID.fromString(path);
                            return true;
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                    })
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.toCollection(LinkedList::new));
        } catch (IOException e) {
            Epitaphs.log.error("Failed to list player directories at {}", storage.getFileName(), e);
            return new LinkedList<>();
        }
    }
    
    ///
    /// Returns a list of names for all backup files for the given player.
    ///
    /// @param server The server instance.
    /// @param player The player UUID.
    ///
    /// @return list of backup file names
    ///
    public static LinkedList<String> listFiles(MinecraftServer server, UUID player) {
        Path storage = DataHandler.getFileStorage(server).resolve(player.toString());
        if (!Files.exists(storage) || !Files.isDirectory(storage)) {
            Epitaphs.log.debug("No player directory found at {}", storage.getFileName());
            return new LinkedList<>();
        }
        
        try (Stream<Path> stream = Files.walk(storage, 1)) {
            return stream.filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(path -> BackupType.ANY.pattern().matcher(path).matches())
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.toCollection(LinkedList::new));
        } catch (IOException e) {
            Epitaphs.log.error("Failed to list player files at {}", storage.getFileName(), e);
            return new LinkedList<>();
        }
    }
    
    ///
    /// Saves all current players to disk.
    ///
    /// @param server The server instance.
    /// @param now    The timestamp of the backup.
    ///
    public static int saveAll(MinecraftServer server, Instant now) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for (ServerPlayer player : players) DataHandler.save(player, now, BackupType.SAVE);
        return players.size();
    }
    
    ///
    /// Purges all outdated backup files.
    ///
    /// @param server The server instance.
    ///
    public static void purgeAll(MinecraftServer server) {
        final int backupMaxDaysAge = EPConfig.Server.getBackupMaxAgeDays();
        if (backupMaxDaysAge <= 0) return;
        
        final Instant cutoff = Instant.now().minus(backupMaxDaysAge, ChronoUnit.DAYS);
        final LinkedList<String> players = DataHandler.listPlayer(server);
        final List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (String player : players) {
            futures.add(CompletableFuture.runAsync(() -> {
                final UUID uuid = UUID.fromString(player);
                Path storage = DataHandler.getFileStorage(server).resolve(uuid.toString());
                try (Stream<Path> stream = Files.walk(storage, 1)) {
                    DataHandler.purgeByTimestamp(uuid, new ArrayList<>(
                            stream.filter(Files::isRegularFile)
                                    .filter(p -> BackupType.ANY.pattern().matcher(p.getFileName().toString()).matches())
                                    .sorted(Comparator.naturalOrder())
                                    .toList()
                    ), cutoff, backupMaxDaysAge);
                } catch (IOException e) {
                    Epitaphs.log.error("Failed to purge outdated files for {}", uuid, e);
                }
            }));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    }
    
    ///
    /// Saves the given player to disk.
    ///
    /// @param player The player instance.
    /// @param now    The timestamp of the backup.
    /// @param type   The type of backup.
    ///
    /// @return {@code 1} if the save was successful, {@code 0} otherwise.
    ///
    public static int save(ServerPlayer player, Instant now, BackupType type) {
        ServerLevel level = player.level();
        MinecraftServer server = level.getServer();
        UUID uuid = player.getUUID();
        Path storage = DataHandler.getFileStorage(server).resolve(uuid.toString());
        
        final CompoundTag data;
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(Epitaphs.log)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, player.registryAccess());
            player.saveWithoutId(output);
            data = output.buildResult();
        }
        
        try {
            Files.createDirectories(storage);
            Path file = type.create(storage, now);
            NbtIo.writeCompressed(data, file);
            Epitaphs.log.debug("Saved player data for {} to {}", player.getGameProfile().name(), file.getFileName());
        } catch (IOException e) {
            Epitaphs.log.error("Failed to save player data for {} at {}", uuid, storage.getFileName(), e);
            return 0;
        }
        
        DataHandler.purge(uuid, storage, type);
        return 1;
    }
    
    ///
    /// Loads the given backup into the given player.
    ///
    /// @param player The player instance.
    /// @param uuid   The backup players uuid.
    /// @param now    The timestamp of the backup.
    /// @param type   The type of backup.
    ///
    /// @return {@code 1} if the load was successful, {@code 0} otherwise.
    ///
    @SuppressWarnings("DuplicatedCode")
    public static int load(ServerPlayer player, UUID uuid, Instant now, BackupType type) {
        Path playerDirectory = DataHandler.getFileStorage(player.level().getServer()).resolve(uuid.toString());
        
        try {
            Path file = type.resolve(playerDirectory, now);
            CompoundTag backup = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            
            final PlayerContainer backupContainer;
            try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(Epitaphs.log)) {
                backupContainer = PlayerContainer.create(TagValueInput.create(reporter, player.registryAccess(), backup));
            }
            PlayerContainer playerContainer = PlayerContainer.create(player);
            List<ItemStack> overflow = playerContainer.merge(backupContainer);
            List<ItemStack> dropped = playerContainer.inventory().insert(overflow);
            Epitaphs.log.debug("Merged data from {} into {}", file.getFileName(), player.getGameProfile().name());
            
            if (!dropped.isEmpty()) for (ItemStack stack : dropped) player.drop(stack, false);
            
            playerContainer.write(player);
            Epitaphs.log.debug("Loaded data from {} for {}", file.getFileName(), player.getGameProfile().name());
            return 1;
            
        } catch (IOException e) {
            Epitaphs.log.error("Failed to load data for {} at {}", player.getUUID(), now.toString(), e);
            return 0;
        }
    }
    
    ///
    /// Resets the player's data to the given backup.
    ///
    /// @param server The server instance.
    /// @param uuid   The player UUID.
    /// @param now    The timestamp of the backup.
    ///
    public static int reset(MinecraftServer server, UUID uuid, Instant now) {
        Path world = DataHandler.getPlayerDataStorage(server);
        Path storage = DataHandler.getFileStorage(server).resolve(uuid.toString());
        
        try {
            Path file = BackupType.ANY.resolve(storage, now);
            CompoundTag data = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            
            Path temp = Files.createTempFile(world, uuid + "-", ".dat");
            NbtIo.writeCompressed(data, temp);
            Path saved = world.resolve(uuid + ".dat");
            Path old = world.resolve(uuid + ".dat_old");
            Util.safeReplaceFile(saved, temp, old);
            Epitaphs.log.debug("Reset offline player data for {} from {}", uuid, file.getFileName());
            
            @Nullable ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(Epitaphs.log)) {
                    player.load(TagValueInput.create(reporter, server.registryAccess(), data));
                    Epitaphs.log.debug("Reloaded live player data for {} from {}", player.getGameProfile().name(), file.getFileName());
                }
            }
            return 1;
            
        } catch (IOException e) {
            Epitaphs.log.error("Failed to set player data for {} at {}", uuid, storage.getFileName(), e);
            return 0;
        }
    }
    
    ///
    /// Restores a player's data from the given backup.
    /// Tries to restore the online player if available, otherwise falls back to offline restore.
    ///
    /// @param server The server instance.
    /// @param uuid   The player UUID.
    /// @param now    The timestamp of the backup.
    /// @param type   The type of backup.
    ///
    /// @return {@code 1} if the restore was successful, {@code 0} otherwise.
    ///
    /// @see OnlineHandler#restore(ServerPlayer, Instant, BackupType)
    /// @see OfflineHandler#restore(MinecraftServer, UUID, Instant, BackupType)
    ///
    public static int restore(MinecraftServer server, UUID uuid, Instant now, BackupType type) {
        final ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player == null) return OfflineHandler.restore(server, uuid, now, type);
        else return OnlineHandler.restore(player, now, type);
    }
    
    ///
    /// Gets a list of items from the given backup.
    /// Tries to gather from the online player if available, otherwise falls back to offline gathering.
    ///
    /// @param server The server instance.
    /// @param uuid   The player UUID.
    /// @param now    The timestamp of the backup.
    /// @param type   The type of backup.
    ///
    /// @return list of items from the backup.
    ///
    /// @see OnlineHandler#gather(ServerPlayer, Instant, BackupType)
    /// @see OfflineHandler#gather(MinecraftServer, UUID, Instant, BackupType)
    ///
    public static List<ItemStack> gather(MinecraftServer server, UUID uuid, Instant now, BackupType type) {
        final ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player == null) return OfflineHandler.gather(server, uuid, now, type);
        else return OnlineHandler.gather(player, now, type);
    }
    
    ///
    /// Invalidates the current grave data
    ///
    /// @param server The server instance
    /// @param uuid   The player UUID
    /// @param now    The timestamp of the backup
    ///
    public static void invalidate(MinecraftServer server, UUID uuid, Instant now) {
        Path playerDirectory = DataHandler.getFileStorage(server).resolve(uuid.toString());
        
        try {
            Path file = BackupType.DEATH.resolve(playerDirectory, now);
            String timestamp = DataHandler.FORMATTER.format(now);
            Path old = playerDirectory.resolve(timestamp + "-death.dat-old");
            Files.move(file, old, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            Epitaphs.log.debug("Invalidated death data for {} at {}", uuid, now);
        } catch (IOException e) {
            Epitaphs.log.warn("Failed to invalidate death data for {} at {}", uuid, now, e);
        }
    }
}
