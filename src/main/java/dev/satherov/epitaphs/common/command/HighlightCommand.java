package dev.satherov.epitaphs.common.command;

import lombok.experimental.UtilityClass;

import dev.satherov.epitaphs.client.lang.EPLanguage;
import dev.satherov.epitaphs.common.component.LocationData;
import dev.satherov.epitaphs.common.component.TrackedLocation;
import dev.satherov.epitaphs.common.data.DataHandler;
import dev.satherov.epitaphs.core.EPRegistry;
import dev.satherov.epitaphs.util.StringUtils;

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
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
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
import java.util.TreeMap;
import java.util.UUID;

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
                        .requires(cs -> cs.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("timestamp", StringArgumentType.string())
                                        .suggests(EPCommands.FILE_BY_PLAYER_PROVIDER)
                                        .executes(ctx -> HighlightCommand.executePlayer(ctx, EntityArgument.getPlayer(ctx, "player")))
                                )
                        )
                )
                .then(Commands.literal("uuid")
                        .requires(cs -> cs.hasPermission(2))
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
        source.sendSuccess(() -> EPLanguage.COMMAND_HIGHLIGHT_CLEAR.text().withStyle(ChatFormatting.GREEN), false);
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
            source.sendFailure(EPLanguage.COMMAND_LIST_EMPTY.text(EPCommands.formatPlayer(player.getGameProfile())).withStyle(ChatFormatting.RED));
            return 0;
        }
        
        final GlobalPos global = positions.lastEntry().getValue();
        return HighlightCommand.execute(ctx, global);
    }
    
    private static int executePlayer(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException {
        final CommandSourceStack source = ctx.getSource();
        final MinecraftServer server = source.getServer();
        
        final String timestamp = StringArgumentType.getString(ctx, "timestamp");
        if (timestamp.isBlank()) {
            player.setData(EPRegistry.TRACKED_LOCATION_DATA, TrackedLocation.ZERO);
            source.sendSuccess(EPLanguage.COMMAND_HIGHLIGHT_CLEAR::text, false);
            return 1;
        }
        
        final LocationData locations = player.getData(EPRegistry.LOCATION_DATA);
        final TreeMap<Instant, GlobalPos> positions = locations.getAll(server);
        if (positions.isEmpty()) {
            source.sendFailure(EPLanguage.COMMAND_LIST_EMPTY.text(EPCommands.formatPlayer(player.getGameProfile())).withStyle(ChatFormatting.RED));
            return 0;
        }
        
        final Instant instant = LocalDateTime.parse(timestamp, DataHandler.FORMATTER).atZone(ZoneOffset.UTC).toInstant();
        final GlobalPos global = positions.get(instant);
        if (global == null) {
            source.sendFailure(EPLanguage.COMMAND_HIGHLIGHT_INVALID.text(timestamp).withStyle(ChatFormatting.RED));
            return 0;
        }
        
        return HighlightCommand.execute(ctx, global);
    }
    
    private static int executeUUID(CommandContext<CommandSourceStack> ctx, UUID uuid) throws CommandSyntaxException {
        final CommandSourceStack source = ctx.getSource();
        final ServerPlayer player = source.getPlayerOrException();
        
        final String timestamp = StringArgumentType.getString(ctx, "timestamp");
        if (timestamp.isBlank()) {
            player.setData(EPRegistry.TRACKED_LOCATION_DATA, TrackedLocation.ZERO);
            source.sendSuccess(EPLanguage.COMMAND_HIGHLIGHT_CLEAR::text, false);
            return 1;
        }
        
        final MinecraftServer server = source.getServer();
        final GameProfile profile = EPCommands.getProfile(server, uuid);
        final Path playerData = DataHandler.getPlayerDataStorage(server).resolve(uuid.toString());
        
        try {
            CompoundTag data = NbtIo.readCompressed(playerData, NbtAccounter.unlimitedHeap());
            CompoundTag attachments = data.getCompound("neoforge:attachments");
            LocationData locations = LocationData.CODEC.decode(NbtOps.INSTANCE, attachments).getOrThrow().getFirst();
            final TreeMap<Instant, GlobalPos> positions = locations.getAll(server);
            if (positions.isEmpty()) {
                source.sendFailure(EPLanguage.COMMAND_LIST_EMPTY.text(EPCommands.formatPlayer(player.getGameProfile())).withStyle(ChatFormatting.RED));
                return 0;
            }
            
            final Instant instant = LocalDateTime.parse(timestamp, DataHandler.FORMATTER).atZone(ZoneOffset.UTC).toInstant();
            final GlobalPos global = positions.get(instant);
            if (global == null) {
                source.sendFailure(EPLanguage.COMMAND_HIGHLIGHT_INVALID.text(timestamp).withStyle(ChatFormatting.RED));
                return 0;
            }
            
            return HighlightCommand.execute(ctx, global);
        } catch (IOException | IllegalStateException e) {
            source.sendFailure(EPLanguage.COMMAND_LIST_FAILURE_FILE.text(EPCommands.formatPlayer(profile)).withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    private static int execute(CommandContext<CommandSourceStack> ctx, GlobalPos global) throws CommandSyntaxException {
        final CommandSourceStack source = ctx.getSource();
        final ServerPlayer player = source.getPlayerOrException();
        
        final BlockPos pos = global.pos();
        final String dimension = global.dimension().location().toString();
        final TrackedLocation tracked = new TrackedLocation(global);
        if (player.getData(EPRegistry.TRACKED_LOCATION_DATA).equals(tracked)) {
            player.setData(EPRegistry.TRACKED_LOCATION_DATA, TrackedLocation.ZERO);
            source.sendSuccess(() -> EPLanguage.COMMAND_HIGHLIGHT_CLEAR.text().withStyle(ChatFormatting.GREEN), false);
            return 1;
        }
        
        player.setData(EPRegistry.TRACKED_LOCATION_DATA, tracked);
        source.sendSuccess(() -> EPLanguage.COMMAND_HIGHLIGHT_SUCCESS.text(
                ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", pos.getX(), pos.getY(), pos.getZ())).withStyle(style -> {
                    style = style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, StringUtils.format("/execute in %s run tp @s %s %s %s", dimension, pos.getX(), pos.getY(), pos.getZ())));
                    style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, EPLanguage.MESSAGE_AUTOFILL_COMMAND.text(ChatFormatting.DARK_GRAY)));
                    style = style.withColor(ChatFormatting.GREEN);
                    return style;
                }),
                ComponentUtils.wrapInSquareBrackets(Component.literal(dimension).withStyle(ChatFormatting.GOLD))
        ), false);
        return 1;
    }
}
