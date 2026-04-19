package dev.satherov.epitaphs.common.command;

import lombok.experimental.UtilityClass;

import dev.satherov.epitaphs.client.lang.EPCommandLang;
import dev.satherov.epitaphs.client.lang.EPMessageLang;
import dev.satherov.epitaphs.common.data.DataHandler;
import dev.satherov.sathlib.network.chat.SLComponent;
import dev.satherov.sathlib.util.SLStringUtils;

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
        return FilesCommand.execute(ctx, uuid, file -> SLStringUtils.format("/epitaphs recover uuid %s %s", uuid, file));
    }
    
    private static int executePlayer(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        final UUID uuid = player.getUUID();
        final String name = player.getGameProfile().name();
        return FilesCommand.execute(ctx, uuid, file -> SLStringUtils.format("/epitaphs recover player %s %s", name, file));
    }
    
    private static int execute(CommandContext<CommandSourceStack> ctx, UUID uuid, Function<String, String> commandFactory) {
        final CommandSourceStack source = ctx.getSource();
        final MinecraftServer server = source.getServer();
        final GameProfile profile = EPCommands.getProfile(server, uuid);
        final List<String> files = DataHandler.listFiles(server, uuid);
        
        if (files.isEmpty()) {
            source.sendFailure(EPCommandLang.COMMAND_FILES_EMPTY.translate(EPCommands.formatPlayer(profile)).style(ChatFormatting.RED));
            return 0;
        }
        
        source.sendSuccess(() -> EPCommandLang.COMMAND_FILES_SUCCESS.translate(EPCommands.formatPlayer(profile)).style(ChatFormatting.GREEN), false);
        for (String file : files)
            source.sendSuccess(() -> SLComponent.of(Component.literal(" - ").append(file)).style(style -> {
                style.color(ChatFormatting.GREEN);
                style.clickEvent(new ClickEvent.SuggestCommand(commandFactory.apply(file)));
                style.hoverEvent(new HoverEvent.ShowText(EPMessageLang.MESSAGE_AUTOFILL_COMMAND.translate(ChatFormatting.DARK_GRAY)));
                return style;
            }), false);
        return 1;
    }
}
