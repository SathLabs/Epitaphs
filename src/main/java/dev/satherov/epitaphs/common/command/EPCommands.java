package dev.satherov.epitaphs.common.command;

import dev.satherov.epitaphs.common.data.DataHandler;
import dev.satherov.sathlib.network.chat.SLComponent;

import net.neoforged.neoforge.common.UsernameCache;

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
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import java.util.LinkedList;
import java.util.UUID;
import java.util.function.Predicate;

public class EPCommands {
    
    public static final PermissionCheck PERMISSION_CHECK = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);
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
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("epitaphs")
                // Commands for everyone
                .then(UUIDCommand.register())
                .then(ListCommand.register())
                .then(HighlightCommand.register())
                // Commands for operators only
                .then(ResetCommand.register().requires(EPCommands.hasPermission()))
                .then(RecoverCommand.register().requires(EPCommands.hasPermission()))
                .then(SaveCommand.register().requires(EPCommands.hasPermission()))
                .then(FilesCommand.register().requires(EPCommands.hasPermission()))
        );
    }
    
    public static Predicate<CommandSourceStack> hasPermission() {
        return Commands.hasPermission(EPCommands.PERMISSION_CHECK);
    }
    
    public static GameProfile getProfile(MinecraftServer server, UUID uuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) return player.getGameProfile();
        String name = UsernameCache.getLastKnownUsername(uuid);
        if (name == null) name = "Unknown";
        return new GameProfile(uuid, name);
    }
    
    public static Component formatPlayer(GameProfile profile) {
        return SLComponent.squareBrackets(Component.literal(profile.name())).style(style -> {
            style.color(ChatFormatting.GOLD);
            style.hoverEvent(new HoverEvent.ShowText(Component.literal(profile.id().toString()).withStyle(ChatFormatting.DARK_GRAY)));
            style.clickEvent(new ClickEvent.CopyToClipboard(profile.id().toString()));
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
}
