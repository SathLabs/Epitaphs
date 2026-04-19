package dev.satherov.epitaphs.common.command;

import lombok.experimental.UtilityClass;

import dev.satherov.epitaphs.client.lang.EPCommandLang;
import dev.satherov.epitaphs.client.lang.EPMessageLang;
import dev.satherov.sathlib.network.chat.SLComponent;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

@UtilityClass
public class UUIDCommand {
    
    ///
    /// Prints out a players uuid into chat
    ///
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        // @formatter:off
        return Commands.literal("uuid")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(UUIDCommand::execute)
                );
        // @formatter:on
    }
    
    private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final CommandSourceStack source = ctx.getSource();
        final ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        final GameProfile profile = player.getGameProfile();
        final String uuid = profile.id().toString();
        final String name = profile.name();
        
        source.sendSuccess(() -> EPCommandLang.COMMAND_UUID_RESULT.translate(
                // Name plus copy to clipboard
                SLComponent.squareBrackets(Component.literal(name)).style(style -> {
                    style.color(ChatFormatting.GOLD);
                    style.clickEvent(new ClickEvent.CopyToClipboard(name));
                    style.hoverEvent(new HoverEvent.ShowText(EPMessageLang.MESSAGE_COPY_TO_CLIPBOARD.translate(ChatFormatting.DARK_GRAY)));
                    return style;
                }),
                // UUID plus copy to clipboard
                SLComponent.squareBrackets(Component.literal(uuid)).style(style -> {
                    style.color(ChatFormatting.GOLD);
                    style.clickEvent(new ClickEvent.CopyToClipboard(uuid));
                    style.hoverEvent(new HoverEvent.ShowText(EPMessageLang.MESSAGE_COPY_TO_CLIPBOARD.translate(ChatFormatting.DARK_GRAY)));
                    return style;
                })
        ), false);
        return 1;
    }
}
