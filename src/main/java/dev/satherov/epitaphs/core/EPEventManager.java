package dev.satherov.epitaphs.core;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.client.lang.EPLanguage;
import dev.satherov.epitaphs.common.block.EPGraveBlock;
import dev.satherov.epitaphs.common.commands.EPRecoverCommand;
import dev.satherov.epitaphs.common.data.EPDataHandler;
import dev.satherov.epitaphs.common.data.EPGraveState;
import dev.satherov.epitaphs.common.data.EPSaveType;
import dev.satherov.epitaphs.common.tile.EPGraveBlockEntity;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.brigadier.CommandDispatcher;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class EPEventManager {

    @SubscribeEvent( priority = EventPriority.LOWEST )
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();

        Optional<BlockPos> safeSpot = EPGraveBlock.findSafeSpot(level, player.blockPosition());
        if (safeSpot.isEmpty()) {
            player.displayClientMessage(EPLanguage.MESSAGE_GRAVE_FAILED.translateFormatted(ChatFormatting.RED), false);
            return;
        }

        BlockPos pos = safeSpot.get();
        BlockState below = level.getBlockState(pos.below());

        if (below.is(BlockTags.REPLACEABLE)) {
            if (level.getBlockState(pos.below(2)).is(BlockTags.REPLACEABLE)) {
                level.setBlockAndUpdate(pos.below(), Blocks.DIRT.defaultBlockState());
            } else {
                pos = pos.below();
            }
        }

        final BlockPos gravePos = pos.immutable();

        CompoundTag data = EPDataHandler.save(player, EPSaveType.DEATH, gravePos);

        if (data.isEmpty()) return;

        level.setBlockAndUpdate(gravePos, EPRegistry.GRAVE.get().defaultBlockState());
        EPGraveBlockEntity grave = new EPGraveBlockEntity(gravePos, level.getBlockState(gravePos));

        grave.saveData(data);
        level.setBlockEntity(grave);


        player.displayClientMessage(Component.empty()
                .append(EPLanguage.MESSAGE_GRAVE_SUCCESS.translate())
                .append(Component.literal(" "))
                .append(ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", gravePos.getX(), gravePos.getY(), gravePos.getZ())).withStyle(style ->
                        style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s %s %s %s".formatted(gravePos.getX(), gravePos.getY(), gravePos.getZ())))
                ).withStyle(ChatFormatting.GOLD)
        ).withStyle(ChatFormatting.GRAY), false);
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(pos -> {
            BlockState state = event.getLevel().getBlockState(pos);
            return state.is(EPRegistry.GRAVE.get());
        });
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        EPRecoverCommand.register(dispatcher);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(level.getBlockEntity(event.getPos()) instanceof EPGraveBlockEntity grave)) return;

        ServerPlayer player = (ServerPlayer) event.getEntity();
        CompoundTag data = grave.getData();
        EPGraveState state = EPDataHandler.load(player, EPSaveType.DEATH, data, false);

        if (state == EPGraveState.FAIL) {
            player.displayClientMessage(EPLanguage.MESSAGE_GRAVE_ERROR.translateFormatted(ChatFormatting.RED), true);
            return;
        }

        if (state == EPGraveState.DENY) {
            String id = data.getString("uuid");
            Player owner = level.getPlayerByUUID(UUID.fromString(id));
            String user = owner == null ? "Unknown" : owner.getName().getString();
            player.displayClientMessage(EPLanguage.MESSAGE_NO_ACCESS.translate(user).withStyle(ChatFormatting.RED), true);
            return;
        }

        player.inventoryMenu.broadcastChanges();
        player.connection.send(new ClientboundContainerSetContentPacket(
                player.inventoryMenu.containerId,
                player.inventoryMenu.incrementStateId(),
                player.inventoryMenu.getItems(),
                player.inventoryMenu.getCarried()
        ));

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS,
                0.4F,
                ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F
        );

        level.removeBlockEntity(event.getPos());
        level.removeBlock(event.getPos(), false);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onSavePlayer(PlayerEvent.SaveToFile event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        EPDataHandler.save(player, EPSaveType.SAVE, null);

        LinkedHashMap<Path, CompoundTag> files = EPDataHandler.loadAll(player);
        if (files.isEmpty()) return;

        List<Path> toDelete = collectFilesForCleanup(files);

        toDelete.forEach(file -> {
            try {
                Files.deleteIfExists(file);
                files.remove(file);
            } catch (IOException e) {
                Epitaphs.LOGGER.warn("Failed to delete data file {}", file, e);
            }
        });
    }

    private static @NotNull List<Path> collectFilesForCleanup(LinkedHashMap<Path, CompoundTag> files) {
        LinkedList<Path> toDelete = new LinkedList<>();

        int saveCount = 0;
        int oldCount = 0;

        List<Map.Entry<Path, CompoundTag>> entries = new ArrayList<>(files.entrySet());
        for (int i = entries.size() - 1; i >= 0; i--) {
            Map.Entry<Path, CompoundTag> entry = entries.get(i);
            Path file = entry.getKey();
            CompoundTag data = entry.getValue();
            String fileName = file.getFileName().toString();

            if (data.isEmpty()) {
                toDelete.add(file);
                continue;
            }

            if (fileName.endsWith("save.dat")) {
                if (++saveCount > 10) {
                    toDelete.add(file);
                }
            } else if (fileName.endsWith(".dat-old")) {
                if (++oldCount > 5) {
                    toDelete.add(file);
                }
            }
        }

        return toDelete;
    }
}
