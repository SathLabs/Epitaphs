package dev.satherov.epitaphs.common.command;

import dev.satherov.epitaphs.client.lang.EPLanguage;
import dev.satherov.epitaphs.common.data.BackupHandler;
import dev.satherov.epitaphs.common.data.EBackupType;
import dev.satherov.epitaphs.core.EPRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerPlayer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class EPCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("epitaphs")
                        .then(recoverCommand())
                        .then(backupCommand())
                        .then(listCommand())
                        .then(latestCommand())
                        .then(filesCommand())
                        .then(uuidCommand())
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> recoverCommand() {
        return Commands.literal("recover")
                .requires(cs -> cs.hasPermission(4))
                .then(Commands.literal("player")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("timestamp", StringArgumentType.string())
                                        .suggests(FILE_SUGGESTER_PLAYER)
                                        .executes(ctx -> recoverPlayer(
                                                ctx, 
                                                StringArgumentType.getString(ctx, "timestamp"), 
                                                false)
                                        )
                                        .then(Commands.argument("force", BoolArgumentType.bool())
                                                .executes(ctx -> recoverPlayer(
                                                        ctx, 
                                                        StringArgumentType.getString(ctx, "timestamp"), 
                                                        BoolArgumentType.getBool(ctx, "force"))
                                                )
                                        )
                                )
                                .executes(ctx -> recoverPlayer(ctx, null, false))
                        )
                )
                .then(Commands.literal("uuid")
                        .then(Commands.argument("uuid", StringArgumentType.string())
                                .suggests(UUID_SUGGESTER)
                                .then(Commands.argument("timestamp", StringArgumentType.string())
                                        .suggests(FILE_SUGGESTER_UUID)
                                        .executes(ctx -> recoverUUID(
                                                ctx, 
                                                StringArgumentType.getString(ctx, "timestamp"), 
                                                false
                                        ))
                                        .then(Commands.argument("force", BoolArgumentType.bool())
                                                .executes(ctx -> recoverUUID(
                                                        ctx, 
                                                        StringArgumentType.getString(ctx, "timestamp"), 
                                                        BoolArgumentType.getBool(ctx, "force"))
                                                )
                                        )
                                )
                                .executes(ctx -> recoverUUID(ctx, null, false))
                        )
                );
    }

    private static int recoverUUID(CommandContext<CommandSourceStack> ctx, String timestamp, boolean force) {
        String uuid = StringArgumentType.getString(ctx, "uuid");
        return recover(ctx, uuid, timestamp, force);
    }
    
    private static int recoverPlayer(CommandContext<CommandSourceStack> ctx, String timestamp, boolean force) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        return recover(ctx, player.getStringUUID(), timestamp, force);
    }
    
    private static int recover(CommandContext<CommandSourceStack> ctx, String uuid, String timestamp, boolean force) {
        int result = BackupHandler.restoreCommand(ctx.getSource().getServer(), ctx.getSource(), uuid, timestamp, force);
        ServerPlayer player = ctx.getSource().getServer().getPlayerList().getPlayer(UUID.fromString(uuid));
        String user = player == null ? uuid : player.getName().getString();
        if (result == 0) {
            ctx.getSource().sendSystemMessage(EPLanguage.COMMAND_RESTORE_SUCCESS.translate(user));
        } else {
            ctx.getSource().sendSystemMessage(EPLanguage.COMMAND_RESTORE_FAILED.translate(user));
        }
        return result;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> backupCommand() {
        return Commands.literal("save")
                .requires(cs -> cs.hasPermission(4))
                .then(Commands.argument("players", EntityArgument.players())
                        .executes(EPCommands::save));
    }

    private static int save(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
        for (ServerPlayer player : players) {
            int result = BackupHandler.save(
                    player,
                    DateTimeFormatter
                            .ofPattern("yyyy-MM-dd-HH-mm-ss")
                            .withZone(ZoneOffset.UTC)
                            .format(Instant.now()),
                    EBackupType.SAVE);
            if (result != 0) {
                ctx.getSource().sendSystemMessage(EPLanguage.COMMAND_BACKUP_FAILED.translate(player.getDisplayName()));
                return -1;
            }
        }
        ctx.getSource().sendSystemMessage(EPLanguage.COMMAND_BACKUP_SUCCESS.translate());
        return 0;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> listCommand() {
        return Commands.literal("list")
                .executes(ctx -> list(ctx, ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(cs -> cs.hasPermission(4))
                        .executes(ctx -> list(ctx, EntityArgument.getPlayer(ctx, "player")))
                );
    }

    private static int list(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        Map<String, List<Map.Entry<String, BlockPos>>> locations = player.getData(EPRegistry.LOCATION_DATA).getGraveLocations(player.serverLevel());
        if (locations.isEmpty()) {
            ctx.getSource().sendSystemMessage(EPLanguage.COMMAND_NOT_FOUND.translate(player.getDisplayName()).withStyle(ChatFormatting.RED));
            return 0;
        }

        locations.forEach((dimension, list) -> {
            ctx.getSource().sendSystemMessage(Component.literal(dimension).append(":").withStyle(ChatFormatting.GOLD));
            list.forEach(entry -> {
                final BlockPos gravePos = entry.getValue().immutable();
                ctx.getSource().sendSystemMessage(Component.empty()
                        .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(entry.getKey()).withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
                        .append(ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", gravePos.getX(), gravePos.getY(), gravePos.getZ())).withStyle(style ->
                                        style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/execute in %s run tp @s %s %s %s".formatted(dimension, gravePos.getX(), gravePos.getY(), gravePos.getZ())))
                                ).withStyle(ChatFormatting.AQUA)
                        )
                );
            });
        });
        return 0;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> latestCommand() {
        return Commands.literal("latest")
                .executes(ctx -> latest(ctx, ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(cs -> cs.hasPermission(4))
                        .executes(ctx -> latest(ctx, EntityArgument.getPlayer(ctx, "player")))
                );

    }

    private static int latest(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        player.getData(EPRegistry.LOCATION_DATA).findLatestGraveLocation(player.serverLevel()).ifPresentOrElse(entry -> {
            final BlockPos pos = entry.pos().immutable();
            ctx.getSource().sendSystemMessage(Component.empty()
                    .append(Component.literal(entry.dimension().location().toString()).append(":").withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
                    .append(ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", pos.getX(), pos.getY(), pos.getZ())).withStyle(style ->
                                    style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/execute in %s run tp @s %s %s %s".formatted(entry.dimension().location(), pos.getX(), pos.getY(), pos.getZ())))
                            ).withStyle(ChatFormatting.AQUA)
                    )
            );
        }, () -> {
            ctx.getSource().sendSystemMessage(EPLanguage.COMMAND_NOT_FOUND.translate(player.getDisplayName()).withStyle(ChatFormatting.RED));
        });
        return 0;
    }
    
    private static LiteralArgumentBuilder<CommandSourceStack> filesCommand() {
        return Commands.literal("files")
                .requires(ctx -> ctx.hasPermission(2))
                .then(Commands.literal("player")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> files(ctx, EntityArgument.getPlayer(ctx, "player").getStringUUID()))
                        )
                )
                .then(Commands.literal("uuid")
                        .then(Commands.argument("uuid", StringArgumentType.string())
                                .suggests(UUID_SUGGESTER)
                                .executes(ctx -> files(ctx, StringArgumentType.getString(ctx, "uuid")))
                        )
                );
    }
    
    private static int files(CommandContext<CommandSourceStack> ctx, String player) {
        List<String> files = BackupHandler.listBackups(ctx.getSource().getServer(), player);
        ctx.getSource().sendSystemMessage(Component.literal("Available backups for ").append(Component.literal(player).withStyle(ChatFormatting.GREEN)));
        files.forEach(file -> ctx.getSource().sendSystemMessage(Component.literal("- ").append(Component.literal(file).withStyle(ChatFormatting.GRAY))));
        return 0;
    }
    
    private static LiteralArgumentBuilder<CommandSourceStack> uuidCommand() {
        return Commands.literal("uuid")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> uuid(ctx, EntityArgument.getPlayer(ctx, "player")))
                )
                .executes(ctx -> uuid(ctx, ctx.getSource().getPlayerOrException()));
    }
    
    private static int uuid(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException {
        ctx.getSource().sendSystemMessage(
                Component.literal(String.format("%s has the UUID: ", player.getGameProfile().getName()))
                        .append(Component.literal(player.getStringUUID())
                                .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, player.getStringUUID())))
                                .withStyle(ChatFormatting.GREEN)
                        ).withStyle(ChatFormatting.GRAY)
        );
        return 0;
    }
    
    private static final SuggestionProvider<CommandSourceStack> FILE_SUGGESTER_PLAYER = (context, builder) -> {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        return getSuggestions(player.getStringUUID(), context, builder);
    };
    
    private static final SuggestionProvider<CommandSourceStack> FILE_SUGGESTER_UUID = (context, builder) -> {
        String uuid = StringArgumentType.getString(context, "uuid");
        return getSuggestions(uuid, context, builder);
    };

    private static CompletableFuture<Suggestions> getSuggestions(String uuid, CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        LinkedList<String> files = BackupHandler.listBackups(context.getSource().getServer(), uuid);
        files.forEach(builder::suggest);
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> UUID_SUGGESTER = (context, builder) -> {
        LinkedList<String> files = BackupHandler.listPlayers(context.getSource().getServer());
        files.forEach(builder::suggest);
        return builder.buildFuture();
    };

}
