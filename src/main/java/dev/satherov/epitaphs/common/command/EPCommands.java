package dev.satherov.epitaphs.common.command;

import dev.satherov.epitaphs.common.data.DataHandler;

import net.neoforged.neoforge.common.UsernameCache;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import java.util.LinkedList;
import java.util.UUID;

public class EPCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("epitaphs")
                // Commands for everyone
                .then(UUIDCommand.register())
                .then(ListCommand.register())
                .then(HighlightCommand.register())
                // Commands for operators only
                .then(ResetCommand.register().requires(css -> css.hasPermission(2)))
                .then(RecoverCommand.register().requires(css -> css.hasPermission(2)))
                .then(SaveCommand.register().requires(css -> css.hasPermission(2)))
                .then(FilesCommand.register().requires(css -> css.hasPermission(2)))
        );
    }
    
    public static GameProfile getProfile(MinecraftServer server, UUID uuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) return player.getGameProfile();
        String name = UsernameCache.getLastKnownUsername(uuid);
        if (name == null) name = "Unknown";
        return new GameProfile(uuid, name);
    }
    
    public static MutableComponent formatPlayer(GameProfile profile) {
        return ComponentUtils.wrapInSquareBrackets(Component.literal(profile.getName())).withStyle(style -> {
            style = style.withColor(ChatFormatting.GOLD);
            style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(profile.getId().toString()).withStyle(ChatFormatting.DARK_GRAY)));
            style = style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, profile.getId().toString()));
            return style;
        });
    }
    
    public static SuggestionProvider<CommandSourceStack> creatFileProvider(UUID uuid) {
        return (ctx, builder) -> {
            final MinecraftServer server = ctx.getSource().getServer();
            final LinkedList<String> files = DataHandler.listFiles(server, uuid);
            files.forEach(builder::suggest);
            return builder.buildFuture();
        };
    }
    
    public static final SuggestionProvider<CommandSourceStack> FILE_BY_UUID_PROVIDER = (ctx, builder) -> {
        final MinecraftServer server = ctx.getSource().getServer();
        final UUID uuid = UuidArgument.getUuid(ctx, "uuid");
        final LinkedList<String> files = DataHandler.listFiles(server, uuid);
        files.forEach(builder::suggest);
        return builder.buildFuture();
    };
    
    public static final SuggestionProvider<CommandSourceStack> FILE_BY_PLAYER_PROVIDER = (ctx, builder) -> {
        final MinecraftServer server = ctx.getSource().getServer();
        final ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        final LinkedList<String> files = DataHandler.listFiles(server, player.getUUID());
        files.forEach(builder::suggest);
        return builder.buildFuture();
    };
    
    public static final SuggestionProvider<CommandSourceStack> FOLDER_UUIDS_PROVIDER = (ctx, builder) -> {
        final MinecraftServer server = ctx.getSource().getServer();
        final LinkedList<String> folders = DataHandler.listPlayer(server);
        folders.forEach(builder::suggest);
        return builder.buildFuture();
    };
}
