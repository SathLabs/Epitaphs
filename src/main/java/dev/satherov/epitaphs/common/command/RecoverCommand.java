package dev.satherov.epitaphs.common.command;

import lombok.experimental.UtilityClass;

import dev.satherov.epitaphs.client.lang.EPCommandLang;
import dev.satherov.epitaphs.common.data.BackupType;
import dev.satherov.epitaphs.common.data.DataHandler;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.server.MinecraftServer;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@UtilityClass
public class RecoverCommand {
    
    ///
    /// Tries to merge inventories from a backup timestamp into the given player
    ///
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        // @formatter:off
        return Commands.literal("recover")
                .then(Commands.literal("player")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("timestamp", StringArgumentType.string())
                                        .suggests(EPCommands.FILE_BY_PLAYER_PROVIDER)
                                        .executes(ctx -> RecoverCommand.execute(ctx, EntityArgument.getPlayer(ctx, "player").getUUID()))
                                )
                        )
                )
                .then(Commands.literal("uuid")
                        .then(Commands.argument("uuid", UuidArgument.uuid())
                                .suggests(EPCommands.FOLDER_UUIDS_PROVIDER)
                                .then(Commands.argument("timestamp", StringArgumentType.string())
                                        .suggests(EPCommands.FILE_BY_UUID_PROVIDER)
                                        .executes(ctx -> RecoverCommand.execute(ctx, UuidArgument.getUuid(ctx, "uuid")))
                                )
                        )
                );
        // @formatter:on
    }
    
    private static int execute(CommandContext<CommandSourceStack> ctx, UUID uuid) {
        final CommandSourceStack source = ctx.getSource();
        final MinecraftServer server = source.getServer();
        final String timestamp = StringArgumentType.getString(ctx, "timestamp");
        final Instant instant = LocalDateTime.parse(timestamp, DataHandler.FORMATTER).atZone(ZoneOffset.UTC).toInstant();
        final GameProfile profile = EPCommands.getProfile(server, uuid);
        int result = DataHandler.restore(server, uuid, instant, BackupType.ANY);
        
        if (result < 1) {
            source.sendFailure(EPCommandLang.COMMAND_RECOVER_SUCCESS.translate(EPCommands.formatPlayer(profile), timestamp).style(ChatFormatting.RED));
            return 0;
        } else {
            source.sendSuccess(() -> EPCommandLang.COMMAND_RECOVER_SUCCESS.translate(EPCommands.formatPlayer(profile), timestamp).style(ChatFormatting.GREEN), true);
            return 1;
        }
    }
}
