package dev.satherov.epitaphs.common.command;

import lombok.experimental.UtilityClass;

import dev.satherov.epitaphs.client.lang.EPCommandLang;
import dev.satherov.epitaphs.common.data.BackupType;
import dev.satherov.epitaphs.common.data.DataHandler;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.time.Instant;
import java.util.List;

@UtilityClass
public class SaveCommand {
    
    ///
    /// Saves all players within the argument target to disk
    ///
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        // @formatter:off
        return Commands.literal("save")
                .then(Commands.argument("players", EntityArgument.players())
                        .executes(SaveCommand::execute)
                );
        // @formatter:on
    }
    
    private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final CommandSourceStack source = ctx.getSource();
        final List<ServerPlayer> players = (List<ServerPlayer>) EntityArgument.getPlayers(ctx, "players");
        final Instant now = Instant.now();
        final String timestamp = DataHandler.FORMATTER.format(now);
        int result = 0;
        for (ServerPlayer player : players) {
            result += DataHandler.save(player, now, BackupType.SAVE);
        }
        
        if (result < 1 && players.size() == 1) {
            final GameProfile profile = players.getFirst().getGameProfile();
            source.sendFailure(EPCommandLang.COMMAND_SAVE_FAILURE_SINGLE.translate(EPCommands.formatPlayer(profile), timestamp).style(ChatFormatting.RED));
            return 0;
        } else if (result < 1 && players.size() > 1) {
            source.sendFailure(EPCommandLang.COMMAND_SAVE_FAILURE_FULL.translate(timestamp).style(ChatFormatting.RED));
            return 0;
        } else if (result != players.size()) {
            source.sendFailure(EPCommandLang.COMMAND_SAVE_FAILURE_PARTIAL.translate(timestamp).style(ChatFormatting.YELLOW));
            return 0;
        } else if (players.size() == 1) {
            final GameProfile profile = players.getFirst().getGameProfile();
            source.sendSuccess(() -> EPCommandLang.COMMAND_SAVE_SUCCESS_SINGLE.translate(EPCommands.formatPlayer(profile), timestamp).style(ChatFormatting.GREEN), true);
            return 1;
        } else {
            source.sendSuccess(() -> EPCommandLang.COMMAND_SAVE_SUCCESS_FULL.translate(timestamp).style(ChatFormatting.GREEN), true);
            return 1;
        }
    }
}
