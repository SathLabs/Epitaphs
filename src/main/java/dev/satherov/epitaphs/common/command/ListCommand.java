package dev.satherov.epitaphs.common.command;

import lombok.experimental.UtilityClass;

import dev.satherov.epitaphs.client.lang.EPLanguage;
import dev.satherov.epitaphs.common.component.LocationData;
import dev.satherov.epitaphs.common.data.DataHandler;
import dev.satherov.epitaphs.core.EPRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@UtilityClass
public class ListCommand {
    
    ///
    /// Lists all grave locations for a given player
    ///
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        // @formatter:off
        return Commands.literal("list")
                .executes(ListCommand::executeSelf)
                .then(Commands.literal("latest")
                        .executes(ListCommand::executeLatest)
                )
                .then(Commands.literal("player")
                        .requires(cs -> cs.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> ListCommand.executePlayer(ctx, EntityArgument.getPlayer(ctx, "player")))
                        )
                )
                .then(Commands.literal("uuid")
                        .requires(cs -> cs.hasPermission(2))
                        .then(Commands.argument("uuid", UuidArgument.uuid())
                                .suggests(EPCommands.FOLDER_UUIDS_PROVIDER)
                                .executes(ctx -> ListCommand.executeUUID(ctx, UuidArgument.getUuid(ctx, "uuid")))
                        )
                );
        // @formatter:on
    }
    
    private static int executeSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final CommandSourceStack source = ctx.getSource();
        final MinecraftServer server = source.getServer();
        final ServerPlayer player = source.getPlayerOrException();
        final LocationData locations = player.getData(EPRegistry.LOCATION_DATA);
        return ListCommand.execute(ctx, player.getGameProfile(), locations.getAll(server));
    }
    
    private static int executeLatest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final CommandSourceStack source = ctx.getSource();
        final MinecraftServer server = source.getServer();
        final ServerPlayer player = source.getPlayerOrException();
        final LocationData locations = player.getData(EPRegistry.LOCATION_DATA);
        final TreeMap<Instant, GlobalPos> positions = locations.getAll(server);
        
        if (positions.isEmpty()) {
            source.sendFailure(EPLanguage.COMMAND_LIST_EMPTY.text(EPCommands.formatPlayer(player.getGameProfile())).withStyle(ChatFormatting.RED));
            return 0;
        }
        
        final Map.Entry<Instant, GlobalPos> entry = positions.lastEntry();
        final Instant time = entry.getKey();
        final GlobalPos global = entry.getValue();
        final String dimension = global.dimension().location().toString();
        final BlockPos pos = global.pos();
        source.sendSuccess(() -> EPLanguage.COMMAND_LIST_LATEST.text(
                Component.empty()
                        .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(DataHandler.ISO8601_FORMATTER.format(time)).withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(" "))
                        .append(ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", pos.getX(), pos.getY(), pos.getZ())).withStyle(style ->
                                style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/execute in %s run tp @s %s %s %s".formatted(dimension, pos.getX(), pos.getY(), pos.getZ())))
                        ).withStyle(ChatFormatting.AQUA)),
                ComponentUtils.wrapInSquareBrackets(Component.literal(dimension)).withStyle(ChatFormatting.GOLD)
        ), false);
        return 1;
    }
    
    private static int executePlayer(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        final MinecraftServer server = ctx.getSource().getServer();
        final LocationData locations = player.getData(EPRegistry.LOCATION_DATA);
        return ListCommand.execute(ctx, player.getGameProfile(), locations.getAll(server));
    }
    
    private static int executeUUID(CommandContext<CommandSourceStack> ctx, UUID uuid) {
        final CommandSourceStack source = ctx.getSource();
        final MinecraftServer server = source.getServer();
        final GameProfile profile = EPCommands.getProfile(server, uuid);
        final Path playerData = DataHandler.getPlayerDataStorage(server).resolve(uuid.toString());
        
        try {
            CompoundTag data = NbtIo.readCompressed(playerData, NbtAccounter.unlimitedHeap());
            CompoundTag attachments = data.getCompound("neoforge:attachments");
            LocationData locations = LocationData.fromAttachments(attachments);
            return ListCommand.execute(ctx, profile, locations.getAll(server));
        } catch (IOException | IllegalStateException e) {
            source.sendFailure(EPLanguage.COMMAND_LIST_FAILURE_FILE.text(EPCommands.formatPlayer(profile)).withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    private static int execute(CommandContext<CommandSourceStack> ctx, GameProfile profile, TreeMap<Instant, GlobalPos> positions) {
        final CommandSourceStack source = ctx.getSource();
        if (positions.isEmpty()) {
            source.sendFailure(EPLanguage.COMMAND_LIST_EMPTY.text(EPCommands.formatPlayer(profile)).withStyle(ChatFormatting.RED));
            return 0;
        }
        
        final Map<String, Map<Instant, BlockPos>> map = new TreeMap<>();
        for (Map.Entry<Instant, GlobalPos> entry : positions.entrySet()) {
            final Instant time = entry.getKey();
            final GlobalPos global = entry.getValue();
            final String dimension = global.dimension().location().toString();
            final BlockPos pos = global.pos();
            map.computeIfAbsent(dimension, k -> new TreeMap<>()).put(time, pos);
        }
        
        source.sendSuccess(() -> EPLanguage.COMMAND_LIST_SUCCESS.text(EPCommands.formatPlayer(profile)).withStyle(ChatFormatting.GREEN), false);
        map.forEach((dimension, entry) -> {
            source.sendSystemMessage(ComponentUtils.wrapInSquareBrackets(Component.literal(dimension)).append(":").withStyle(ChatFormatting.GOLD));
            entry.forEach((time, pos) -> {
                source.sendSystemMessage(Component.empty()
                        .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(DataHandler.ISO8601_FORMATTER.format(time)).withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(" "))
                        .append(ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", pos.getX(), pos.getY(), pos.getZ())).withStyle(style -> {
                            style = style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/execute in %s run tp @s %s %s %s".formatted(dimension, pos.getX(), pos.getY(), pos.getZ())));
                            style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, EPLanguage.MESSAGE_AUTOFILL_COMMAND.text(ChatFormatting.DARK_GRAY)));
                            style = style.withColor(ChatFormatting.AQUA);
                            return style;
                        })));
            });
        });
        return 1;
    }
}
