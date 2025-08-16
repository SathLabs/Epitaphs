package dev.satherov.epitaphs.common.data;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.EpitaphsConfig;
import dev.satherov.epitaphs.client.lang.EPLanguage;
import dev.satherov.epitaphs.compat.CompatHandler;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BackupHandler {

    public static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}");
    public static final Pattern FILE_PATTERN = Pattern.compile(DATE_PATTERN + "-(?:death|save)\\.dat(?:-old)?$");

    public static LinkedList<String> listPlayers(MinecraftServer server) {
        return listEntries(server, true, null);
    }

    public static LinkedList<String> listBackups(MinecraftServer server, String uuid) {
        return listEntries(server, false, uuid);
    }

    private static LinkedList<String> listEntries(MinecraftServer server, boolean listPlayers, String uuid) {
        if (server == null) {
            Epitaphs.LOGGER.error("Failed to list {} because server is unavailable.", listPlayers ? "players" : "backups");
            return new LinkedList<>();
        }

        Path storage = server.getWorldPath(LevelResource.ROOT)
                .normalize()
                .toAbsolutePath()
                .resolve("data")
                .resolve(Epitaphs.MOD_ID);

        Path target = listPlayers ? storage : storage.resolve(uuid);

        if (!Files.exists(target) || !Files.isDirectory(target)) {
            String entityType = listPlayers ? "backup directory" : "player directory";
            String identifier = listPlayers ? "storage" : uuid;
            Epitaphs.LOGGER.debug("No {} found for '{}'", entityType, identifier);
            return new LinkedList<>();
        }

        try (Stream<Path> walk = Files.walk(target, 1)) {
            LinkedList<String> results = walk
                    .filter(listPlayers ? Files::isDirectory : Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(string -> listPlayers ? isValidUuid(string) : FILE_PATTERN.matcher(string).matches())
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.toCollection(LinkedList::new))
                    .reversed();

            if (results.isEmpty()) {
                String entityType = listPlayers ? "player directories" : "backup files";
                String identifier = listPlayers ? "storage" : uuid;
                Epitaphs.LOGGER.debug("No {} found for '{}'", entityType, identifier);
            }

            return results;

        } catch (IOException e) {
            String entityType = listPlayers ? "players" : "backups";
            String identifier = listPlayers ? "storage" : uuid;
            Epitaphs.LOGGER.error("Failed to list {} for '{}'", entityType, identifier, e);
        }
        return new LinkedList<>();
    }

    private static boolean isValidUuid(String string) {
        try {
            UUID.fromString(string);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    public static int save(ServerPlayer player, String timestamp, EBackupType type) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            Epitaphs.LOGGER.error("Failed to save data for player '{}' because server is unavailable", player.getScoreboardName());
            return -1;
        }

        Path storage = server.getWorldPath(LevelResource.ROOT)
                .normalize()
                .toAbsolutePath()
                .resolve("data")
                .resolve(Epitaphs.MOD_ID)
                .resolve(player.getUUID().toString());

        CompoundTag tag = player.saveWithoutId(new CompoundTag());

        if (tag.isEmpty()) {
            Epitaphs.LOGGER.error("Player '{}' has no data", player.getScoreboardName());
            return -1;
        }

        try {

            Files.createDirectories(storage);

            NbtIo.writeCompressed(tag, storage.resolve(timestamp + "-" + type.getSerializedName() + ".dat"));
            if (type == EBackupType.DEATH) {
                CompatHandler.clearCurio(player);
                player.getInventory().clearContent();
            }

        } catch (IOException e) {
            Epitaphs.LOGGER.error("Failed to write data for player '{}' to disk", player.getScoreboardName(), e);
            return -1;
        }

        Pattern pattern = switch (type) {
            case DEATH -> Pattern.compile(DATE_PATTERN.pattern() + "-death\\.dat-old");
            case SAVE -> Pattern.compile(DATE_PATTERN.pattern() + "-save\\.dat(?:-old)?$");
        };

        try (Stream<Path> walk = Files.walk(storage, 1)) {
            List<Path> files = new ArrayList<>(
                    walk.filter(Files::isRegularFile)
                            .filter(path -> pattern.matcher(path.getFileName().toString()).matches())
                            .sorted(Comparator.naturalOrder())
                            .toList()
            );

            while (files.size() >= (type == EBackupType.SAVE ? EpitaphsConfig.getMaxBackups() : EpitaphsConfig.getMaxOld())) {
                Path file = files.removeFirst();
                Files.deleteIfExists(file);
                Epitaphs.LOGGER.debug("Removed old save file '{}'", file.getFileName());
            }

        } catch (IOException e) {
            Epitaphs.LOGGER.error("Failed to clean up old saves for player '{}'", player.getScoreboardName(), e);
            return -1;
        }

        return 0;
    }

    public static boolean saveAll(MinecraftServer server) {
        Path storage = server.getWorldPath(LevelResource.ROOT)
                .normalize()
                .toAbsolutePath()
                .resolve("data")
                .resolve(Epitaphs.MOD_ID);

        Pattern pattern = Pattern.compile(DATE_PATTERN.pattern() + "-save\\.dat(?:-old)?$");
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            Epitaphs.LOGGER.warn("No players found, skipping save");
            return false;
        }

        for (ServerPlayer player : players) {
            Path target = storage.resolve(player.getUUID().toString());
            CompoundTag tag = player.saveWithoutId(new CompoundTag());

            try {
                Files.createDirectories(target);
                String timestamp = DateTimeFormatter
                        .ofPattern("yyyy-MM-dd-HH-mm-ss")
                        .withZone(ZoneOffset.UTC)
                        .format(Instant.now());

                NbtIo.writeCompressed(tag, target.resolve(timestamp + "-save.dat"));
            } catch (IOException e) {
                Epitaphs.LOGGER.error("Failed to write data for player '{}' to disk", player.getScoreboardName(), e);
            }
        }

        try (Stream<Path> walk = Files.walk(storage, 2)) {
            Map<Path, List<Path>> existing = walk.filter(Files::isRegularFile)
                    .filter(path -> pattern.matcher(path.getFileName().toString()).matches())
                    .collect(Collectors.groupingBy(Path::getParent));

            for (List<Path> files : existing.values()) {
                files.sort(Comparator.naturalOrder());
                while (files.size() > EpitaphsConfig.getMaxBackups()) {
                    Path file = files.removeFirst();
                    Files.deleteIfExists(file);
                }
            }
        } catch (IOException e) {
            Epitaphs.LOGGER.error("Failed to clean up old saves", e);
            return false;
        }
        return true;
    }

    public static CompoundTag load(MinecraftServer server, String uuid, String timestamp) {
        CompoundTag data = new CompoundTag();
        if (server == null) {
            Epitaphs.LOGGER.error("Failed to load data for player with uuid '{}' because server is unavailable", uuid);
            return data;
        }

        if (timestamp.isBlank() || !(DATE_PATTERN.matcher(timestamp).matches() || FILE_PATTERN.matcher(timestamp).matches())) {
            Epitaphs.LOGGER.error("Invalid backup timestamp '{}'", timestamp);
            return data;
        }

        Path target = server.getWorldPath(LevelResource.ROOT)
                .normalize()
                .toAbsolutePath()
                .resolve("data")
                .resolve(Epitaphs.MOD_ID)
                .resolve(uuid);
        
        if (!Files.exists(target) || !Files.isDirectory(target)) {
            Epitaphs.LOGGER.debug("No backup directory found for uuid '{}'", uuid);
            return data;
        }

        final Path file;

        try (Stream<Path> files = Files.list(target)) {

            file = files.filter(p -> FileSystems.getDefault()
                            .getPathMatcher("regex:" + FILE_PATTERN.pattern())
                            .matches(p.getFileName()) && p.getFileName().toString().startsWith(timestamp))
                    .toList()
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (file == null) throw new IOException();
        } catch (IOException e) {
            Epitaphs.LOGGER.error("No backup file found for uuid '{}' at '{}'", uuid, timestamp);
            return data;
        }

        try {
            data = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            if (data.isEmpty()) throw new IOException();
        } catch (IOException e) {
            Epitaphs.LOGGER.error("Failed to read file '{}'", file.getFileName(), e);
        }
        return data;
    }

    public static int restore(ServerPlayer player, String uuid, String timestamp, boolean clear) {
        CompoundTag data = load(player.getServer(), uuid, timestamp);
        if (data.isEmpty()) {
            Epitaphs.LOGGER.warn("Cannot restore uuid '{}' because data is empty", uuid);
            return -1;
        }
        return restoreOnline(player, data, clear);
    }

    public static int restoreCommand(MinecraftServer server, CommandSourceStack source, String uuid, String timestamp, boolean clear) {
        
        CompoundTag data = load(server, uuid, timestamp);
        if (data.isEmpty()) {
            Epitaphs.LOGGER.warn("Cannot restore uuid '{}' because data is empty", uuid);
            return -1;
        }

        ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(uuid));
        if (player != null) {
            Epitaphs.LOGGER.debug("Restoring data for player '{}'", player.getScoreboardName());
            return restoreOnline(player, data, clear);
        } else {
            Epitaphs.LOGGER.debug("Restoring data for offline player '{}'", uuid);
            return restoreOffline(server, source, uuid, data, clear);
        }
    }
    
    private static int restoreOnline(ServerPlayer player, CompoundTag data, boolean clear) {
 
        ListTag inventory = data.getList("Inventory", Tag.TAG_COMPOUND);

        if (clear) player.getInventory().clearContent();

        CompatHandler.loadCurioInventory(player, data, clear);

        if (quickLoad(player, inventory)) return 0;

        List<ItemStack> overflow = new ArrayList<>();
        NonNullList<ItemStack> items = NonNullList.withSize(36, ItemStack.EMPTY);
        NonNullList<ItemStack> armor = NonNullList.withSize(4, ItemStack.EMPTY);
        NonNullList<ItemStack> offhand = NonNullList.withSize(1, ItemStack.EMPTY);

        for (int i = 0; i < inventory.size(); i++) {
            CompoundTag tag = inventory.getCompound(i);
            int j = tag.getByte("Slot") & 255;
            ItemStack itemstack = ItemStack.parse(player.registryAccess(), tag).orElse(ItemStack.EMPTY);

            if (j < items.size()) {
                items.set(j, itemstack);
            } else if (j >= 100 && j < armor.size() + 100) {
                armor.set(j - 100, itemstack);
            } else if (j >= 150 && j < offhand.size() + 150) {
                offhand.set(j - 150, itemstack);
            }
        }

        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;
            if (player.getInventory().items.get(i).isEmpty()) {
                player.getInventory().items.set(i, stack);
            } else {
                overflow.add(stack);
            }
        }

        for (int i = 0; i < player.getInventory().armor.size(); i++) {
            ItemStack stack = armor.get(i);
            if (stack.isEmpty()) continue;
            if (player.getInventory().armor.get(i).isEmpty()) {
                player.getInventory().armor.set(i, stack);
            } else {
                overflow.add(stack);
            }
        }

        for (int i = 0; i < player.getInventory().offhand.size(); i++) {
            ItemStack stack = offhand.get(i);
            if (stack.isEmpty()) continue;
            if (player.getInventory().offhand.get(i).isEmpty()) {
                player.getInventory().offhand.set(i, stack);
            } else {
                overflow.add(stack);
            }
        }

        for (ItemStack stack : overflow) {
            if (!player.getInventory().add(stack)) {
                ItemEntity item = player.drop(stack, false);
                if (item != null) item.setNoPickUpDelay();
            }
        }

        return 0;
    }
    
    private static int restoreOffline(MinecraftServer server, CommandSourceStack source, String uuid, CompoundTag data, boolean clear) {
        File directory = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();
        File file = new File(directory, uuid + ".dat");

        CompoundTag root = new CompoundTag();
        
        if (file.exists() && file.isFile()) {
            try {
                root = NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap());
            } catch (Exception exception) {
                Epitaphs.LOGGER.warn("Failed to access playerdata for uuid '{}'", uuid);
                return -1;
            }
        }

        ListTag rootInventory;
        if (clear) {
            root.remove("Inventory");
            rootInventory = new ListTag();
            root.put("Inventory", rootInventory);
        } else {
            rootInventory = root.getList("Inventory", Tag.TAG_COMPOUND);
        }

        ListTag inventory = data.getList("Inventory", Tag.TAG_COMPOUND);
        List<CompoundTag> failedInserts = new ArrayList<>();

        Set<Integer> occupied = new HashSet<>();
        for (int i = 0; i < rootInventory.size(); i++) {
            CompoundTag existing = rootInventory.getCompound(i);
            int slot = existing.getByte("Slot") & 255;
            occupied.add(slot);
        }

        for (int i = 0; i < inventory.size(); i++) {
            CompoundTag itemTag = inventory.getCompound(i);
            int slot = itemTag.getByte("Slot") & 255;

            if (!occupied.contains(slot)) {
                rootInventory.add(itemTag);
                occupied.add(slot);
            } else {
                failedInserts.add(itemTag);
            }
        }

        List<CompoundTag> failed = new ArrayList<>();
        for (CompoundTag failedItem : failedInserts) {
            boolean inserted = false;

            for (int slot = 0; slot < 36; slot++) {
                if (!occupied.contains(slot)) {
                    CompoundTag newItemTag = failedItem.copy();
                    newItemTag.putByte("Slot", (byte) slot);
                    rootInventory.add(newItemTag);
                    occupied.add(slot);
                    inserted = true;
                    break;
                }
            }

            if (!inserted) {
                failed.add(failedItem);
            }
        }

 
        if (!failed.isEmpty()) {
            Epitaphs.LOGGER.warn("Could not insert {} items for uuid '{}' - no empty slots available:", failed.size(), uuid);
            
            if (source != null && source.isPlayer()) {
                ServerPlayer player = source.getPlayer();
                
                source.sendSystemMessage(EPLanguage.COMMAND_RESTORE_OVERFLOW.translate(uuid).withStyle(ChatFormatting.RED));
                
                for (CompoundTag tag : failed) {
                    ItemStack stack = ItemStack.parse(server.registryAccess(), tag).orElse(ItemStack.EMPTY);
                    if (stack.isEmpty()) continue;
                    if (!player.getInventory().add(stack)) {
                        player.drop(stack, false);
                    }
                }
                
            } else {
                Epitaphs.LOGGER.error("The following items were lost:");
                for (CompoundTag tag : failed) {
                    int slot = tag.getByte("Slot") & 255;
                    String item = tag.getString("id");
                    int count = tag.getByte("count");
                    Epitaphs.LOGGER.warn(" - {} x{} (slot: {})", item, count, slot);
                }
            }
        }
        
        try {
            NbtIo.writeCompressed(root, file.toPath());
            Epitaphs.LOGGER.debug("Successfully restored items for uuid '{}'", uuid);
        } catch (IOException e) {
            Epitaphs.LOGGER.error("Failed to write updated playerdata for uuid '{}'", uuid, e);
            return -1;
        }

        return 0;
    }

    private static boolean quickLoad(ServerPlayer player, ListTag data) {
        if (player.getInventory().isEmpty()) {
            player.getInventory().load(data);
            return true;
        }
        return false;
    }

    public static List<ItemStack> getContents(MinecraftServer server, String uuid, String timeStamp) {
        return getContents(server, load(server, uuid, timeStamp));
    }

    public static List<ItemStack> getContents(MinecraftServer server, CompoundTag root) {
        ListTag data = root.getList("Inventory", Tag.TAG_COMPOUND);
        List<ItemStack> stacks = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            CompoundTag tag = data.getCompound(i);
            ItemStack itemstack = ItemStack.parse(server.registryAccess(), tag).orElse(ItemStack.EMPTY);
            stacks.add(itemstack);
        }

        stacks.addAll(CompatHandler.loadCurioContents(server, root));

        return stacks;
    }

}
