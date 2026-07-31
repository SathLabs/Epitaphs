package dev.satherov.epitaphs.common.data;

import lombok.experimental.UtilityClass;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.common.command.EPCommands;
import dev.satherov.epitaphs.common.container.CuriosContainer;
import dev.satherov.epitaphs.common.container.PlayerContainer;
import dev.satherov.epitaphs.common.menu.PreviewData;
import dev.satherov.epitaphs.compat.CuriosHandler;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import com.mojang.authlib.GameProfile;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@UtilityClass
public class PreviewHandler {
    
    private static final float DEFAULT_MAX_HEALTH = 20.0F;
    private static final int MAX_LISTED_BACKUPS = 200;
    
    ///
    /// The result of loading a backup for previewing.
    ///
    /// @param container The items stored in the backup.
    /// @param data      The metadata rendered around the items.
    ///
    public record Snapshot(PlayerContainer container, PreviewData data) { }
    
    ///
    /// Loads the given backup without modifying it.
    ///
    /// @param server The server instance.
    /// @param uuid   The UUID of the player who owns the backup.
    /// @param now    The timestamp of the backup.
    /// @param type   The type of backup.
    ///
    /// @return The loaded snapshot, or `null` if the backup could not be read.
    ///
    public static @Nullable Snapshot load(MinecraftServer server, UUID uuid, Instant now, BackupType type) {
        final Path storage = DataHandler.getFileStorage(server).resolve(uuid.toString());
        
        try {
            final Path file = type.resolve(storage, now);
            final CompoundTag backup = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            final RegistryAccess access = server.registryAccess();
            final GameProfile profile = EPCommands.getProfile(server, uuid);
            
            try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(Epitaphs.log)) {
                final ValueInput input = TagValueInput.create(reporter, access, backup);
                final PlayerContainer container = PlayerContainer.create(input);
                final PreviewData data = new PreviewData(
                        new PreviewData.Profile(uuid, profile.name(), now.toEpochMilli(), file.getFileName().toString()),
                        PreviewHandler.vitals(input),
                        PreviewHandler.location(input),
                        input.getIntOr("SelectedItemSlot", 0),
                        PreviewHandler.curios(container.curios()),
                        PreviewHandler.backups(server, uuid)
                );
                
                Epitaphs.log.debug("Loaded preview data from {} for {}", file.getFileName(), uuid);
                return new Snapshot(container, data);
            }
            
        } catch (IOException e) {
            Epitaphs.log.error("Failed to load preview for {} at {}", uuid, now, e);
            return null;
        }
    }
    
    private static PreviewData.Vitals vitals(ValueInput input) {
        return new PreviewData.Vitals(
                input.getFloatOr("Health", PreviewHandler.DEFAULT_MAX_HEALTH),
                PreviewHandler.maxHealth(input),
                input.getFloatOr("AbsorptionAmount", 0.0F),
                input.getIntOr("foodLevel", 20),
                input.getFloatOr("foodSaturationLevel", 5.0F),
                input.getIntOr("XpLevel", 0),
                input.getFloatOr("XpP", 0.0F),
                input.getIntOr("XpTotal", 0)
        );
    }
    
    // Vanilla still writes the game type with the legacy codec, so reading it back needs the same one.
    @SuppressWarnings("deprecation")
    private static PreviewData.Location location(ValueInput input) {
        return new PreviewData.Location(
                input.read("Dimension", Level.RESOURCE_KEY_CODEC),
                input.read("Pos", Vec3.CODEC).orElse(Vec3.ZERO),
                input.read("Rotation", Vec2.CODEC).orElse(Vec2.ZERO),
                input.getIntOr("Air", 300),
                input.getIntOr("Score", 0),
                input.read("playerGameType", GameType.LEGACY_ID_CODEC).orElse(GameType.SURVIVAL)
        );
    }
    
    ///
    /// Lists every other backup this player has, newest first, so the preview can offer them without
    /// another round trip. Capped so the menu's open data stays well inside its size limit.
    ///
    /// @param server The server instance.
    /// @param uuid   The UUID of the player who owns the backups.
    ///
    private static List<String> backups(MinecraftServer server, UUID uuid) {
        final List<String> files = new ArrayList<>(DataHandler.listFiles(server, uuid));
        Collections.reverse(files);
        return files.size() > PreviewHandler.MAX_LISTED_BACKUPS ? List.copyOf(files.subList(0, PreviewHandler.MAX_LISTED_BACKUPS)) : List.copyOf(files);
    }
    
    private static List<PreviewData.CurioSection> curios(CuriosContainer curios) {
        if (!CuriosHandler.isLoaded()) return List.of();
        
        final List<PreviewData.CurioSection> sections = new ArrayList<>();
        for (String identifier : curios.identifiers()) {
            final int items = curios.size(identifier, false);
            final int cosmetics = curios.size(identifier, true);
            if (items <= 0 && cosmetics <= 0) continue;
            sections.add(new PreviewData.CurioSection(identifier, items, cosmetics));
        }
        return sections;
    }
    
    ///
    /// Recreates the stored maximum health of the backup, since attributes are only saved as their
    /// base value plus the modifiers that were applied at the time.
    ///
    /// @param input The backup data.
    ///
    /// @return The maximum health of the backup.
    ///
    private static float maxHealth(ValueInput input) {
        final List<AttributeInstance.Packed> attributes = input.read("attributes", AttributeInstance.Packed.LIST_CODEC).orElse(List.of());
        for (AttributeInstance.Packed attribute : attributes) {
            if (attribute.attribute().value() != Attributes.MAX_HEALTH.value()) continue;
            
            double base = attribute.baseValue();
            for (AttributeModifier modifier : attribute.modifiers()) {
                if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) base += modifier.amount();
            }
            
            double result = base;
            for (AttributeModifier modifier : attribute.modifiers()) {
                if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) result += base * modifier.amount();
            }
            for (AttributeModifier modifier : attribute.modifiers()) {
                if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) result *= 1.0 + modifier.amount();
            }
            
            return (float) Math.max(1.0, attribute.attribute().value().sanitizeValue(result));
        }
        
        return PreviewHandler.DEFAULT_MAX_HEALTH;
    }
}
