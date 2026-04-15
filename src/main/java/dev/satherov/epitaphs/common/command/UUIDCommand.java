package dev.satherov.epitaphs.common.command;

import lombok.experimental.UtilityClass;

import dev.satherov.epitaphs.client.lang.EPLanguage;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
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
        final String uuid = profile.getId().toString();
        final String name = profile.getName();
        
        source.sendSuccess(() -> EPLanguage.COMMAND_UUID_RESULT.text(
                // Name plus copy to clipboard
                ComponentUtils.wrapInSquareBrackets(Component.literal(name)).withStyle(style -> {
                    style = style.withColor(ChatFormatting.GOLD);
                    style = style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, name));
                    style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, EPLanguage.MESSAGE_COPY_TO_CLIPBOARD.text(ChatFormatting.DARK_GRAY)));
                    return style;
                }),
                // UUID plus copy to clipboard
                ComponentUtils.wrapInSquareBrackets(Component.literal(uuid)).withStyle(style -> {
                    style = style.withColor(ChatFormatting.GOLD);
                    style = style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, uuid));
                    style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, EPLanguage.MESSAGE_COPY_TO_CLIPBOARD.text(ChatFormatting.DARK_GRAY)));
                    return style;
                })
        ), false);
        return 1;
    }
}
