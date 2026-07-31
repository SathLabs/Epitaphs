package dev.satherov.epitaphs.common.command;

import lombok.experimental.UtilityClass;

import dev.satherov.epitaphs.client.lang.EPCommandLang;
import dev.satherov.epitaphs.common.component.SlotStackList;
import dev.satherov.epitaphs.common.container.CuriosContainer;
import dev.satherov.epitaphs.common.container.InventoryContainer;
import dev.satherov.epitaphs.common.container.PlayerContainer;
import dev.satherov.epitaphs.common.data.BackupType;
import dev.satherov.epitaphs.common.data.DataHandler;
import dev.satherov.epitaphs.common.data.PreviewHandler;
import dev.satherov.epitaphs.common.menu.PreviewData;
import dev.satherov.epitaphs.common.menu.PreviewMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.stream.IntStream;

@UtilityClass
public class PreviewCommand {
    
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        // @formatter:off
        return Commands.literal("preview")
                .then(Commands.literal("player")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("timestamp", StringArgumentType.string())
                                        .suggests(EPCommands.FILE_BY_PLAYER_PROVIDER)
                                        .executes(ctx -> PreviewCommand.execute(ctx, EntityArgument.getPlayer(ctx, "player").getUUID()))
                                )
                        )
                )
                .then(Commands.literal("uuid")
                        .then(Commands.argument("uuid", UuidArgument.uuid())
                                .suggests(EPCommands.FOLDER_UUIDS_PROVIDER)
                                .then(Commands.argument("timestamp", StringArgumentType.string())
                                        .suggests(EPCommands.FILE_BY_UUID_PROVIDER)
                                        .executes(ctx -> PreviewCommand.execute(ctx, UuidArgument.getUuid(ctx, "uuid")))
                                )
                        )
                );
        // @formatter:on
    }
    
    private static int execute(CommandContext<CommandSourceStack> ctx, UUID uuid) {
        final CommandSourceStack source = ctx.getSource();
        final MinecraftServer server = source.getServer();
        final String timestamp = StringArgumentType.getString(ctx, "timestamp");
        final Matcher matcher = DataHandler.DATE_PATTERN.matcher(timestamp);
        if (!matcher.find()) throw new DateTimeParseException("Could not parse '" + timestamp + "' to a valid save file", timestamp, 0);
        
        final Instant instant = LocalDateTime.parse(matcher.group(), DataHandler.FORMATTER).atZone(ZoneOffset.UTC).toInstant();
        final GameProfile profile = EPCommands.getProfile(server, uuid);
        final @Nullable PreviewHandler.Snapshot snapshot = PreviewHandler.load(server, uuid, instant, BackupType.ANY);
        
        if (snapshot == null) {
            source.sendFailure(EPCommandLang.COMMAND_PREVIEW_FAILURE.translate(EPCommands.formatPlayer(profile), timestamp).style(ChatFormatting.RED));
            return 0;
        }
        
        // No one to open the screen for, so print it to the console
        final @Nullable ServerPlayer viewer = source.getPlayer();
        if (viewer == null) {
            PreviewCommand.print(source, snapshot);
            return 1;
        }
        
        viewer.openMenu(
                new SimpleMenuProvider(
                        (containerId, _, _) -> new PreviewMenu(containerId, snapshot.data(), snapshot.container()),
                        EPCommandLang.COMMAND_PREVIEW_TITLE.translate(profile.name())
                ),
                buffer -> PreviewData.STREAM_CODEC.encode(buffer, snapshot.data())
        );
        
        source.sendSuccess(() -> EPCommandLang.COMMAND_PREVIEW_SUCCESS.translate(EPCommands.formatPlayer(profile), timestamp).style(ChatFormatting.GREEN), false);
        return 1;
    }
    
    // ==================== CONSOLE ====================
    
    private static final String[] ARMOR = { "feet", "legs", "chest", "head" };
    private static final String UNKNOWN = "unknown";
    
    private static void print(CommandSourceStack source, PreviewHandler.Snapshot snapshot) {
        final PreviewData data = snapshot.data();
        final PreviewData.Profile profile = data.profile();
        final PreviewData.Vitals vitals = data.vitals();
        final PreviewData.Location location = data.location();
        final PlayerContainer container = snapshot.container();
        final InventoryContainer inventory = container.inventory();
        final CuriosContainer curios = container.curios();
        
        PreviewCommand.line(source, "Backup of " + profile.name() + " " + profile.uuid());
        PreviewCommand.entry(source, "File", profile.file());
        PreviewCommand.entry(source, "Taken", DataHandler.ISO8601_FORMATTER.format(Instant.ofEpochMilli(profile.timestamp())) + " UTC");
        PreviewCommand.entry(source, "Dimension", location.dimension().map(key -> key.identifier().toString()).orElse(PreviewCommand.UNKNOWN));
        PreviewCommand.entry(source, "Position", Mth.floor(location.position().x()) + " " + Mth.floor(location.position().y()) + " " + Mth.floor(location.position().z()));
        PreviewCommand.entry(source, "Game mode", location.gameType().getName());
        PreviewCommand.entry(source, "Health", PreviewCommand.health(vitals));
        PreviewCommand.entry(source, "Food", vitals.food() + " (" + PreviewCommand.decimal(vitals.saturation()) + " saturation)");
        PreviewCommand.entry(source, "Experience", "level " + vitals.experienceLevel() + " (" + vitals.experienceTotal() + " total)");
        PreviewCommand.entry(source, "Score", String.valueOf(location.score()));
        
        PreviewCommand.section(source, "Inventory", PreviewCommand.dense(inventory.items()), null);
        PreviewCommand.section(source, "Armor", PreviewCommand.dense(inventory.armor()), PreviewCommand.ARMOR);
        PreviewCommand.section(source, "Offhand", PreviewCommand.dense(inventory.offhand()), null);
        
        for (String identifier : curios.identifiers()) {
            PreviewCommand.section(source, "Curios / " + identifier, curios.stacks(identifier, false), null);
            if (curios.size(identifier, true) > 0) {
                PreviewCommand.section(source, "Curios / " + identifier + ", cosmetic", curios.stacks(identifier, true), null);
            }
        }
        
        if (!container.toolbelt().isEmpty()) {
            PreviewCommand.section(source, "Tool belt", PreviewCommand.dense(container.toolbelt().stack()), null);
        }
    }
    
    private static void section(CommandSourceStack source, String title, List<ItemStack> stacks, String @Nullable [] labels) {
        final List<Integer> filled = IntStream.range(0, stacks.size())
                .filter(slot -> !stacks.get(slot).isEmpty())
                .boxed()
                .toList();
        
        if (filled.isEmpty()) {
            PreviewCommand.line(source, title + " (empty)");
            return;
        }
        
        PreviewCommand.line(source, title + " (" + filled.size() + (filled.size() == 1 ? " stack)" : " stacks)"));
        for (int slot : filled) {
            final ItemStack stack = stacks.get(slot);
            final String label = labels != null && slot < labels.length ? labels[slot] : String.valueOf(slot);
            PreviewCommand.line(source, String.format(
                    Locale.ROOT,
                    "  [%3s] %3d x %s%s",
                    label,
                    stack.getCount(),
                    BuiltInRegistries.ITEM.getKey(stack.getItem()),
                    stack.has(DataComponents.CUSTOM_NAME) ? " \"" + stack.getHoverName().getString() + "\"" : ""
            ));
        }
    }
    
    private static String health(PreviewData.Vitals vitals) {
        final String health = PreviewCommand.decimal(vitals.health()) + " / " + PreviewCommand.decimal(vitals.maxHealth());
        return vitals.absorption() > 0.0F ? health + " (+" + PreviewCommand.decimal(vitals.absorption()) + " absorption)" : health;
    }
    
    private static List<ItemStack> dense(SlotStackList list) {
        return IntStream.range(0, list.size()).mapToObj(list::getStack).toList();
    }
    
    private static String decimal(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
    
    private static void entry(CommandSourceStack source, String label, String value) {
        PreviewCommand.line(source, String.format(Locale.ROOT, "  %-10s %s", label, value));
    }
    
    private static void line(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
    }
}
