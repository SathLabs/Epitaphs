package dev.satherov.epitaphs.common.command;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.client.lang.EPLanguage;
import dev.satherov.epitaphs.common.command.suggestion.EPSuggestionBuilder;
import dev.satherov.epitaphs.common.command.suggestion.EPSuggestionProvider;
import dev.satherov.epitaphs.common.data.BackupHandler;
import dev.satherov.epitaphs.common.data.EBackupType;
import dev.satherov.epitaphs.core.EPRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
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

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class EPCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("epitaphs")
                        .then(recoverCommand())
                        .then(backupCommand())
                        .then(listCommand())
                        .then(latestCommand())

        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> recoverCommand() {
        return Commands.literal("recover")
                .requires(cs -> cs.hasPermission(4))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("timestamp", StringArgumentType.string())
                                .suggests(FILE_SUGGESTER)
                                .executes(ctx -> recover(ctx, false))
                                .then(Commands.argument("force", BoolArgumentType.bool())
                                        .executes(ctx -> recover(ctx,  BoolArgumentType.getBool(ctx, "force"))))
                        )
                );
    }

    private static int recover(CommandContext<CommandSourceStack> ctx, boolean force) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String timestamp = StringArgumentType.getString(ctx, "timestamp");
        int result = BackupHandler.restore(player, timestamp, force);
        if (result == 0) {
            ctx.getSource().sendSystemMessage(EPLanguage.COMMAND_RESTORE_SUCCESS.translate(player.getDisplayName()));
        } else {
            ctx.getSource().sendSystemMessage(EPLanguage.COMMAND_RESTORE_FAILED.translate(player.getDisplayName()));
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
                        .executes(ctx -> list(ctx, EntityArgument.getPlayer(ctx, "player")))
                );
    }

    private static int list(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException {
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
                        .executes(ctx -> latest(ctx, EntityArgument.getPlayer(ctx, "player")))
                );

    }

    private static int latest(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException {
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

    private static final SuggestionProvider<CommandSourceStack> FILE_SUGGESTER = (context, builder) -> {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            LinkedList<String> files = BackupHandler.listBackups(player);
            files.forEach(builder::suggest);
        } catch (CommandSyntaxException e) {
            Epitaphs.LOGGER.warn("Failed to suggest backups", e);
        }
        return builder.buildFuture();
    };

}
