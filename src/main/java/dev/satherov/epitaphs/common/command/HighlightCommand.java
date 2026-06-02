package dev.satherov.epitaphs.common.command;

import lombok.experimental.UtilityClass;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.client.lang.EPCommandLang;
import dev.satherov.epitaphs.client.lang.EPMessageLang;
import dev.satherov.epitaphs.common.component.LocationData;
import dev.satherov.epitaphs.common.component.TrackedLocation;
import dev.satherov.epitaphs.common.data.DataHandler;
import dev.satherov.epitaphs.core.EPRegistry;
import dev.satherov.sathlib.network.chat.SLComponent;
import dev.satherov.sathlib.util.SLStringUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;

@SuppressWarnings("DuplicatedCode")
@UtilityClass
public class HighlightCommand {
    
    ///
    /// Allows the player to track a grave location which will show as a highlight
    ///
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        // @formatter:off
        return Commands.literal("highlight")
                .then(Commands.literal("clear")
                        .executes(HighlightCommand::executeClear)
                )
                .then(Commands.literal("latest")
                        .executes(HighlightCommand::executeLatest)
                )
                .then(Commands.literal("deaths")
                        .then(Commands.argument("timestamp", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    final CommandSourceStack source = ctx.getSource();
                                    final ServerPlayer player = source.getPlayerOrException();
                                    return EPCommands.creatFileProvider(player.getUUID()).getSuggestions(ctx, builder);
                                })
                                .executes(HighlightCommand::executeSelf)
                        )
                )
                .then(Commands.literal("player")
                        .requires(EPCommands.hasPermission())
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("timestamp", StringArgumentType.string())
                                        .suggests(EPCommands.FILE_BY_PLAYER_PROVIDER)
                                        .executes(ctx -> HighlightCommand.executePlayer(ctx, EntityArgument.getPlayer(ctx, "player")))
                                )
                        )
                )
                .then(Commands.literal("uuid")
                        .requires(EPCommands.hasPermission())
                        .then(Commands.argument("uuid", UuidArgument.uuid())
                                .suggests(EPCommands.FOLDER_UUIDS_PROVIDER)
                                .then(Commands.argument("timestamp", StringArgumentType.string())
                                        .suggests(EPCommands.FILE_BY_UUID_PROVIDER)
                                        .executes(ctx -> HighlightCommand.executeUUID(ctx, UuidArgument.getUuid(ctx, "uuid")))
                                )
                        )
                );
        // @formatter:on
    }
    
    private static int executeClear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final CommandSourceStack source = ctx.getSource();
        final ServerPlayer player = source.getPlayerOrException();
        player.setData(EPRegistry.TRACKED_LOCATION_DATA, TrackedLocation.ZERO);
        source.sendSuccess(() -> EPCommandLang.COMMAND_HIGHLIGHT_CLEAR.translate().style(ChatFormatting.GREEN), false);
        return 1;
    }
    
    private static int executeSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final CommandSourceStack source = ctx.getSource();
        final ServerPlayer player = source.getPlayerOrException();
        return HighlightCommand.executePlayer(ctx, player);
    }
    
    private static int executeLatest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final CommandSourceStack source = ctx.getSource();
        final MinecraftServer server = source.getServer();
        final ServerPlayer player = source.getPlayerOrException();
        final LocationData locations = player.getData(EPRegistry.LOCATION_DATA);
        final TreeMap<Instant, GlobalPos> positions = locations.getAll(server);
        if (positions.isEmpty()) {
            source.sendFailure(EPCommandLang.COMMAND_LIST_EMPTY.translate(EPCommands.formatPlayer(player.getGameProfile())).style(ChatFormatting.RED));
            return 0;
        }
        
        final GlobalPos global = positions.lastEntry().getValue();
        HighlightCommand.execute(ctx, global);
        return 1;
    }
    
    private static int executePlayer(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException {
        final CommandSourceStack source = ctx.getSource();
        final MinecraftServer server = source.getServer();
        
        final String timestamp = StringArgumentType.getString(ctx, "timestamp");
        if (timestamp.isBlank()) {
            player.setData(EPRegistry.TRACKED_LOCATION_DATA, TrackedLocation.ZERO);
            source.sendSuccess(() -> EPCommandLang.COMMAND_HIGHLIGHT_CLEAR.translate(ChatFormatting.GREEN), false);
            return 1;
        }
        
        final LocationData locations = player.getData(EPRegistry.LOCATION_DATA);
        final TreeMap<Instant, GlobalPos> positions = locations.getAll(server);
        if (positions.isEmpty()) {
            source.sendFailure(EPCommandLang.COMMAND_LIST_EMPTY.translate(EPCommands.formatPlayer(player.getGameProfile())).style(ChatFormatting.RED));
            return 0;
        }
        
        final Matcher matcher = DataHandler.DATE_PATTERN.matcher(timestamp);
        if (!matcher.find()) throw new DateTimeParseException("Could not parse '" + timestamp + "' to a valid save file", timestamp, 0);
        
        final Instant instant = LocalDateTime.parse(matcher.group(), DataHandler.FORMATTER).atZone(ZoneOffset.UTC).toInstant();
        final GlobalPos global = positions.get(instant);
        if (global == null) {
            source.sendFailure(EPCommandLang.COMMAND_HIGHLIGHT_INVALID.translate(timestamp).style(ChatFormatting.RED));
            return 0;
        }
        
        HighlightCommand.execute(ctx, global);
        return 1;
    }
    
    private static int executeUUID(CommandContext<CommandSourceStack> ctx, UUID uuid) throws CommandSyntaxException {
        final CommandSourceStack source = ctx.getSource();
        final ServerPlayer player = source.getPlayerOrException();
        
        final String timestamp = StringArgumentType.getString(ctx, "timestamp");
        if (timestamp.isBlank()) {
            player.setData(EPRegistry.TRACKED_LOCATION_DATA, TrackedLocation.ZERO);
            source.sendSuccess(() -> EPCommandLang.COMMAND_HIGHLIGHT_CLEAR.translate(ChatFormatting.GREEN), false);
            return 1;
        }
        
        final MinecraftServer server = source.getServer();
        final GameProfile profile = EPCommands.getProfile(server, uuid);
        final Path playerData = DataHandler.getPlayerDataStorage(server).resolve(uuid + ".dat");
        
        try {
            CompoundTag data = NbtIo.readCompressed(playerData, NbtAccounter.unlimitedHeap());
            CompoundTag attachments = data.getCompound("neoforge:attachments").orElse(new CompoundTag());
            LocationData locations = LocationData.fromAttachments(attachments);
            final TreeMap<Instant, GlobalPos> positions = locations.getAll(server);
            if (positions.isEmpty()) {
                source.sendFailure(EPCommandLang.COMMAND_LIST_EMPTY.translate(EPCommands.formatPlayer(player.getGameProfile())).style(ChatFormatting.RED));
                return 0;
            }
            
            final Matcher matcher = DataHandler.DATE_PATTERN.matcher(timestamp);
            if (!matcher.find()) throw new DateTimeParseException("Could not parse '" + timestamp + "' to a valid save file", timestamp, 0);
            
            final Instant instant = LocalDateTime.parse(matcher.group(), DataHandler.FORMATTER).atZone(ZoneOffset.UTC).toInstant();
            final GlobalPos global = positions.get(instant);
            if (global == null) {
                source.sendFailure(EPCommandLang.COMMAND_HIGHLIGHT_INVALID.translate(timestamp).style(ChatFormatting.RED));
                return 0;
            }
            
            HighlightCommand.execute(ctx, global);
            return 1;
        } catch (IOException | IllegalStateException e) {
            source.sendFailure(EPCommandLang.COMMAND_LIST_FAILURE_FILE.translate(EPCommands.formatPlayer(profile)).style(ChatFormatting.RED));
            Epitaphs.log.warn("Failed to read player data for {}", uuid, e);
            return 0;
        }
    }
    
    private static void execute(CommandContext<CommandSourceStack> ctx, GlobalPos global) throws CommandSyntaxException {
        final CommandSourceStack source = ctx.getSource();
        final ServerPlayer player = source.getPlayerOrException();
        
        final BlockPos pos = global.pos();
        final String dimension = global.dimension().identifier().toString();
        final TrackedLocation tracked = new TrackedLocation(global);
        if (player.getData(EPRegistry.TRACKED_LOCATION_DATA).equals(tracked)) {
            player.setData(EPRegistry.TRACKED_LOCATION_DATA, TrackedLocation.ZERO);
            source.sendSuccess(() -> EPCommandLang.COMMAND_HIGHLIGHT_CLEAR.translate().style(ChatFormatting.GREEN), false);
            return;
        }
        
        player.setData(EPRegistry.TRACKED_LOCATION_DATA, tracked);
        source.sendSuccess(() -> EPCommandLang.COMMAND_HIGHLIGHT_SUCCESS.translate(
                SLComponent.squareBrackets(SLComponent.pos(pos)).style(style -> {
                    style.color(ChatFormatting.GREEN);
                    style.clickEvent(new ClickEvent.SuggestCommand(SLStringUtils.format("/execute in %s run tp @s %s %s %s", dimension, pos.getX(), pos.getY(), pos.getZ())));
                    style.hoverEvent(new HoverEvent.ShowText(EPMessageLang.MESSAGE_AUTOFILL_COMMAND.translate(ChatFormatting.DARK_GRAY)));
                    return style;
                }),
                SLComponent.squareBrackets(Component.literal(dimension).withStyle(ChatFormatting.GOLD))
        ), false);
    }
}
