package dev.satherov.epitaphs.common.command;

import lombok.experimental.UtilityClass;

import dev.satherov.epitaphs.client.lang.EPLanguage;
import dev.satherov.epitaphs.common.data.DataHandler;
import dev.satherov.epitaphs.util.StringUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@UtilityClass
public class FilesCommand {
    
    ///
    /// Lists all available backup files for a player
    ///
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        // @formatter:off
        return Commands.literal("files")
                .then(Commands.literal("player")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> FilesCommand.executePlayer(ctx, EntityArgument.getPlayer(ctx, "player")))
                        )
                )
                .then(Commands.literal("uuid")
                        .then(Commands.argument("uuid", UuidArgument.uuid())
                                .suggests(EPCommands.FOLDER_UUIDS_PROVIDER)
                                .executes(ctx -> FilesCommand.executeUUID(ctx, UuidArgument.getUuid(ctx, "uuid")))
                        )
                );
        // @formatter:on
    }
    
    private static int executeUUID(CommandContext<CommandSourceStack> ctx, UUID uuid) {
        return FilesCommand.execute(ctx, uuid, file -> StringUtils.format("/epitaphs recover uuid %s %s", uuid, file));
    }
    
    private static int executePlayer(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        final UUID uuid = player.getUUID();
        final String name = player.getGameProfile().getName();
        return FilesCommand.execute(ctx, uuid, file -> StringUtils.format("/epitaphs recover player %s %s", name, file));
    }
    
    private static int execute(CommandContext<CommandSourceStack> ctx, UUID uuid, Function<String, String> commandFactory) {
        final CommandSourceStack source = ctx.getSource();
        final MinecraftServer server = source.getServer();
        final GameProfile profile = EPCommands.getProfile(server, uuid);
        final List<String> files = DataHandler.listFiles(server, uuid);
        
        if (files.isEmpty()) {
            source.sendFailure(EPLanguage.COMMAND_FILES_EMPTY.text(EPCommands.formatPlayer(profile)).withStyle(ChatFormatting.RED));
            return 0;
        }
        
        source.sendSuccess(() -> EPLanguage.COMMAND_FILES_SUCCESS.text(EPCommands.formatPlayer(profile)).withStyle(ChatFormatting.GREEN), false);
        for (String file : files)
            source.sendSuccess(() -> Component.literal(" - ").append(file).withStyle(style -> {
                style.withColor(ChatFormatting.GREEN);
                style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, commandFactory.apply(file)));
                style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, EPLanguage.MESSAGE_AUTOFILL_COMMAND.text(ChatFormatting.DARK_GRAY)));
                return style;
            }), false);
        return 1;
    }
}
