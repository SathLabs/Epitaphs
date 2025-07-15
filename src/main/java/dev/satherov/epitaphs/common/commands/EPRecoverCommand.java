package dev.satherov.epitaphs.common.commands;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.client.lang.EPLanguage;
import dev.satherov.epitaphs.common.data.EPDataHandler;
import dev.satherov.epitaphs.common.data.EPGraveState;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class EPRecoverCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("epitaphs")
                        .requires(cs -> cs.hasPermission(4))
                        .then(Commands.literal("recover")
                                .then(Commands.literal("timestamp")
                                        .then(Commands.argument("timestamp", StringArgumentType.string())
                                                .suggests(FILE_SUGGESTER)
                                                .executes(ctx -> recover(ctx, false))
                                                .then(Commands.literal("force")
                                                        .then(Commands.argument("force", BoolArgumentType.bool())
                                                                .executes(ctx -> recover(ctx, BoolArgumentType.getBool(ctx, "force")))
                                                        )
                                                )
                                        )
                                )
                                .then(Commands.literal("player")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .then(Commands.argument("timestamp", StringArgumentType.string())
                                                        .suggests(FILE_SUGGESTER)
                                                        .executes(ctx -> recover(ctx, EntityArgument.getPlayer(ctx, "player"), false))
                                                        .then(Commands.literal("force")
                                                                .then(Commands.argument("force", BoolArgumentType.bool())
                                                                        .executes(ctx -> recover(ctx, EntityArgument.getPlayer(ctx, "player"),  BoolArgumentType.getBool(ctx, "force")))
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )

        );
    }

    private static int recover(CommandContext<CommandSourceStack> ctx, boolean force) {
        try {
            return recover(ctx, ctx.getSource().getPlayerOrException(), force);
        } catch (Exception e) {
            Epitaphs.LOGGER.error("Failed to recover data file: ", e);
            return 0;
        }
    }


    private static int recover(CommandContext<CommandSourceStack> ctx, ServerPlayer player, boolean force) {
        try {
            if(!EPDataHandler.hasDirectory(player)) {
                Epitaphs.LOGGER.warn("Player {} has no grave data directory", player.getScoreboardName());
                return 0;
            }

            Path file = EPDataHandler.getDirectory(ctx.getSource().getLevel(), player).resolve(StringArgumentType.getString(ctx, "timestamp"));
            CompoundTag data = EPDataHandler.loadFromFile(file, player, false);
            EPGraveState state = EPDataHandler.load(player, data, force);
            return switch (state) {
                case FAIL, DENY -> {
                    if (ctx.getSource().isPlayer()) {
                        ServerPlayer source = ctx.getSource().getPlayer();
                        if (source != null) source.displayClientMessage(EPLanguage.MESSAGE_GRAVE_ERROR.translateFormatted(ChatFormatting.RED), true);
                    }
                    Epitaphs.LOGGER.warn("Failed to recover data file {} for player {}", file, player.getScoreboardName());
                    yield 0;
                }
                case SUCCESS -> 1;
            };
        } catch (Exception e) {
            Epitaphs.LOGGER.error("Failed to recover data file: ", e);
            return 0;
        }
    }

    private static final SuggestionProvider<CommandSourceStack> UUID_SUGGESTER = (ctx, builder) -> {

        CommandSourceStack source = ctx.getSource();
        Path folder = source.getServer()
                .getWorldPath(LevelResource.ROOT)
                .getParent()
                .resolve("data")
                .resolve("epitaphs");

        folder.forEach(path -> {
            if (!Files.isDirectory(path)) return;
            builder.suggest(path.getFileName().toString());
        });

        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> FILE_SUGGESTER = (ctx, builder) -> {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            LinkedHashMap<Path, CompoundTag> files = EPDataHandler.loadAll(player);
            List<Path> paths = new ArrayList<>(files.keySet());
            for (int i = paths.size() - 1; i >= 0; i--) {
                builder.suggest(paths.get(i).getFileName().toString());
            }
        } catch (CommandSyntaxException ignored) {
        }
        return builder.buildFuture();
    };
}
