package dev.satherov.epitaphs.common.data;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.compat.EPCompatHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EPDataHandler {

    public static CompoundTag save(ServerPlayer player, EPSaveType type, @Nullable BlockPos pos) {
        boolean death = type == EPSaveType.DEATH;
        CompoundTag root = new CompoundTag();

        if (pos != null) {
            CompoundTag position = new CompoundTag();
            position.putInt("x", pos.getX());
            position.putInt("y", pos.getY());
            position.putInt("z", pos.getZ());
            root.put("position", position);
        }

        ListTag inventory = player.getInventory().save(new ListTag());
        if (!inventory.isEmpty()) {
            root.put("inventory", inventory);
        }
        ListTag curio = EPCompatHandler.saveCurioInventory(player, death);
        if (!curio.isEmpty()) {
            root.put("curio", curio);
        }
        CompoundTag test = root.copy();
        test.remove("position");
        if (test.isEmpty()) {
            return root;
        }

        CompoundTag data = new CompoundTag();
        String timestamp = DateTimeFormatter
                .ofPattern("yyyy-MM-dd-HH-mm-ss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());

        data.putString("uuid", player.getUUID().toString());
        data.putString("timestamp", timestamp);

        MinecraftServer server = player.getServer();
        if (server == null) {
            Epitaphs.LOGGER.error("Failed to save data for {} because server is unavailable", player.getScoreboardName());
            return data;
        }

        Path dir = getDirectory(server, player);
        Path file = dir.resolve(timestamp + "-" + type.getSerializedName() + ".dat");

        try {
            Files.createDirectories(dir);
            Files.createFile(file);
            NbtIo.write(root, file);
        } catch (IOException e) {
            Epitaphs.LOGGER.error("Failed to save data for {}", player.getScoreboardName(), e);
        }
        if (death) player.getInventory().clearContent();
        return data;
    }

    public static LinkedHashMap<Path, CompoundTag> loadAll(ServerPlayer player) {
        return loadAll(player, EPSaveType.ANY);
    }

    public static LinkedHashMap<Path, CompoundTag> loadAll(ServerPlayer player, EPSaveType type) {
        LinkedHashMap<Path, CompoundTag> files = new LinkedHashMap<>();
        MinecraftServer server = player.getServer();
        if (server == null) {
            Epitaphs.LOGGER.error("Failed to load data for {} because server is unavailable", player.getScoreboardName());
            return files;
        }
        Path dir = getDirectory(server, player);
        if (!Files.exists(dir)) return files;

        try (var stream = Files.list(dir)) {
            stream.forEach(file -> {
                if (type != EPSaveType.ANY && !file.getFileName().toString().endsWith(type.getSerializedName() + ".dat")) return;
                try {
                    CompoundTag data = NbtIo.read(file);
                    files.put(file, data);
                } catch (IOException e) {
                    Epitaphs.LOGGER.error("Failed to load data file '{}'", file, e);
                }
            });
        } catch (IOException e) {
            Epitaphs.LOGGER.error("Failed to list data files for {}", player.getScoreboardName(), e);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        Pattern pattern = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2})");

        return files.entrySet().stream()
                .sorted(Comparator.<Map.Entry<Path, CompoundTag>, LocalDateTime>comparing(e -> {
                    String filename = e.getKey().getFileName().toString();
                    Matcher matcher = pattern.matcher(filename);
                    if (!matcher.find()) {
                        try {
                            return LocalDateTime.parse(matcher.group(1), formatter);
                        } catch (DateTimeParseException ignored) { }
                    }
                    return LocalDateTime.MIN;
                }).reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    public static CompoundTag load(ServerLevel level, EPSaveType type, CompoundTag tag) {
        return load(level, type, tag, false);
    }

    public static CompoundTag load(ServerLevel level, EPSaveType type, CompoundTag tag, boolean clear) {
        String uuid = tag.getString("uuid");
        String timestamp = tag.getString("timestamp");

        if (uuid.isBlank() || timestamp.isBlank()) {
            Epitaphs.LOGGER.warn("Cannot load data because uuid is '{}' and timestamp is '{}'", uuid, timestamp);
            return new CompoundTag();
        }

        Path dir = getDirectory(level, uuid);
        return loadFromFile(dir, timestamp, type, uuid, clear);
    }

    public static EPGraveState load(ServerPlayer player, EPSaveType type, CompoundTag tag, boolean clear) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            Epitaphs.LOGGER.error("Failed to load data for {} because server is unavailable", player.getScoreboardName());
            return EPGraveState.FAIL;
        }

        String uuid = tag.getString("uuid");
        String timestamp = tag.getString("timestamp");
        if (uuid.isBlank() || timestamp.isBlank()) {
            Epitaphs.LOGGER.warn("Cannot load data for {} because uuid is '{}' and timestamp is '{}'", player.getScoreboardName(), uuid, timestamp);
            return EPGraveState.FAIL;
        }

        if (!player.getUUID().toString().equals(uuid) && !player.hasPermissions(3)) {
            return EPGraveState.DENY;
        }

        Path dir = getDirectory(server, player);
        CompoundTag data = loadFromFile(dir, timestamp, type, player.getScoreboardName(), true);
        return load(player, data, clear);
    }

    public static EPGraveState load(ServerPlayer player, CompoundTag data, boolean clear) {
        if (data.isEmpty()) return EPGraveState.FAIL;

        try {
            ListTag inventory = data.getList("inventory", Tag.TAG_COMPOUND);
            ListTag curio = data.getList("curio", Tag.TAG_COMPOUND);

            if (clear) player.getInventory().clearContent();

            if(!quickLoad(player, inventory)) {
                List<ItemStack> overflow = loadInventory(player, inventory);
                for (ItemStack stack : overflow) {
                    if (player.getInventory().add(stack)) {
                        ItemEntity item = player.drop(stack, false);
                        if (item != null) item.setNoPickUpDelay();
                    }
                }
            }

            EPCompatHandler.loadCurioInventory(player, curio, clear);
            return EPGraveState.SUCCESS;

        } catch (Exception e) {
            Epitaphs.LOGGER.error("Failed to process inventory data for {}", player.getScoreboardName(), e);
            return EPGraveState.FAIL;
        }
    }


    public static CompoundTag loadFromFile(Path dir, String timestamp, EPSaveType type, String uuid, boolean deleteAfterRead) {

        for (String ext : type.getExtensions()) {
            Path file = dir.resolve(timestamp + ext);
            if (Files.exists(file)) {
                CompoundTag data = loadFromFile(file, uuid, deleteAfterRead);
                if (!data.isEmpty()) return data;
            }
        }

        Epitaphs.LOGGER.error("Failed to load data for {} because file at {} with {} and type {} does not exist", uuid, dir, timestamp, type.getSerializedName());
        return new CompoundTag();
    }

    public static CompoundTag loadFromFile(Path file, ServerPlayer player, boolean deleteAfterRead) {
        return loadFromFile(file, player.getUUID().toString(), deleteAfterRead);
    }

    public static CompoundTag loadFromFile(Path file, String uuid, boolean deleteAfterRead) {

        try {
            CompoundTag data = NbtIo.read(file);
            if (data == null) {
                Epitaphs.LOGGER.error("Failed to load data for {} because file '{}' is empty", uuid, file);
                return new CompoundTag();
            }

            if (deleteAfterRead) Files.deleteIfExists(file);

            return data;

        } catch (IOException e) {
            Epitaphs.LOGGER.error("Failed to load data for {}", uuid, e);
            return new CompoundTag();
        }
    }

    public static List<ItemStack> loadContents(ServerLevel level, CompoundTag root) {
        ListTag data = root.getList("inventory", Tag.TAG_COMPOUND);
        List<ItemStack> stacks = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            CompoundTag tag = data.getCompound(i);
            ItemStack itemstack = ItemStack.parse(level.registryAccess(), tag).orElse(ItemStack.EMPTY);
            stacks.add(itemstack);
        }

        stacks.addAll(EPCompatHandler.loadCurioContents(level, root));

        return stacks;
    }

    private static boolean quickLoad(ServerPlayer player, ListTag data) {
        if (player.getInventory().isEmpty()) {
            player.getInventory().load(data);
            return true;
        }
        return false;
    }

    private static List<ItemStack> loadInventory(ServerPlayer player, ListTag data) {
        List<ItemStack> overflow = new ArrayList<>();
        NonNullList<ItemStack> items = NonNullList.withSize(player.getInventory().items.size(), ItemStack.EMPTY);
        NonNullList<ItemStack> armor = NonNullList.withSize(player.getInventory().armor.size(), ItemStack.EMPTY);
        NonNullList<ItemStack> offhand = NonNullList.withSize(player.getInventory().offhand.size(), ItemStack.EMPTY);

        for (int i = 0; i < data.size(); i++) {
            CompoundTag tag = data.getCompound(i);
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

        return overflow;
    }

    public static boolean hasDirectory(ServerPlayer player) {
        Path folder = player.serverLevel().getServer()
                .getWorldPath(LevelResource.ROOT)
                .getParent()
                .resolve("data")
                .resolve("epitaphs");

        if (!Files.exists(folder)) {
            return false;
        }

        try (var stream = Files.list(folder)) {
            return stream.anyMatch(path -> Files.isDirectory(path) && path.getFileName().toString().equals(player.getStringUUID()));
        } catch (IOException e) {
            Epitaphs.LOGGER.error("Failed to check directory for {}", player.getScoreboardName(), e);
            return false;
        }
    }

    public static Path getDirectory(ServerLevel level, String uuid) {
        return getDirectory(level.getServer()).resolve(uuid);
    }

    public static Path getDirectory(MinecraftServer server, String uuid) {
        return getDirectory(server).resolve(uuid);
    }

    public static Path getDirectory(ServerLevel level, Player player) {
        return getDirectory(level.getServer()).resolve(player.getUUID().toString());
    }

    public static Path getDirectory(MinecraftServer server, Player player) {
       return getDirectory(server).resolve(player.getUUID().toString());
    }

    private static Path getDirectory(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT)
                .getParent()
                .resolve("data")
                .resolve("epitaphs");
    }
}
