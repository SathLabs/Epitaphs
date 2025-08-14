package dev.satherov.epitaphs.core;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.EpitaphsConfig;
import dev.satherov.epitaphs.client.lang.EPLanguage;
import dev.satherov.epitaphs.common.block.GraveBlock;
import dev.satherov.epitaphs.common.command.EPCommands;
import dev.satherov.epitaphs.common.component.EPGraveDataAttachment;
import dev.satherov.epitaphs.common.component.EPSoulboundAttachment;
import dev.satherov.epitaphs.common.data.BackupHandler;
import dev.satherov.epitaphs.common.data.EBackupType;
import dev.satherov.epitaphs.common.data.SoulboundHandler;
import dev.satherov.epitaphs.common.tile.GraveBlockEntity;
import dev.satherov.epitaphs.compat.CompatHandler;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.brigadier.CommandDispatcher;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EPEventManager {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ServerLevel level = player.serverLevel();

        String timestamp = DateTimeFormatter
                .ofPattern("yyyy-MM-dd-HH-mm-ss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());

        SoulboundHandler.handleSoulbound(player);

        if (player.getInventory().isEmpty() && CompatHandler.isCurioEmpty(player)) return;

        if (BackupHandler.save(player, timestamp, EBackupType.DEATH) != 0) return;

        Optional<BlockPos> safeSpot = GraveBlock.findSafeSpot(level, player.blockPosition());
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

        level.setBlockAndUpdate(gravePos, EPRegistry.GRAVE.get().defaultBlockState());
        GraveBlockEntity grave = new GraveBlockEntity(gravePos, level.getBlockState(gravePos));

        grave.setData(EPRegistry.GRAVE_DATA, grave.getData(EPRegistry.GRAVE_DATA).create(player, timestamp));
        level.setBlockEntity(grave);

        player.displayClientMessage(Component.empty()
                .append(EPLanguage.MESSAGE_GRAVE_SUCCESS.translate())
                .append(Component.literal(" "))
                .append(ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", gravePos.getX(), gravePos.getY(), gravePos.getZ())).withStyle(style ->
                                style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/execute in %s run tp @s %s %s %s".formatted(level.dimension().location(), gravePos.getX(), gravePos.getY(), gravePos.getZ())))
                        ).withStyle(ChatFormatting.GOLD)
                ).withStyle(ChatFormatting.GRAY), false);

        player.setData(EPRegistry.LOCATION_DATA, player.getData(EPRegistry.LOCATION_DATA).addGraveLocation(player, timestamp, gravePos));
    }

    @SubscribeEvent
    private static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SoulboundHandler.restoreSoulbound(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private static void onLootEvent(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        MinecraftServer server = level.getServer();

        Optional<GlobalPos> position = player.getData(EPRegistry.LOCATION_DATA).findLatestGraveLocation(level);
        if (position.isEmpty() || position.get().dimension().equals(level.dimension())) return;
        final BlockPos gravePos = position.get().pos().immutable();
        if (!(level.getBlockEntity(gravePos) instanceof GraveBlockEntity grave)) return;
        EPGraveDataAttachment graveData = grave.getData(EPRegistry.GRAVE_DATA);

        List<ItemStack> saved = BackupHandler.getContents(server, graveData.getOwner(), graveData.getTimestamp());
        List<ItemStack> drops = new ArrayList<>(event.getDrops().stream().map(ItemEntity::getItem).toList());
        drops.removeAll(saved);
        if (drops.isEmpty()) return;
        graveData.saveAdditional(drops);
        grave.setData(EPRegistry.GRAVE_DATA, graveData);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        EPSoulboundAttachment attachment = player.getData(EPRegistry.SOULBOUND_DATA);

        int xp = SoulboundHandler.handleXpSoulbound(player);
        if (xp > 0) {
            int experience = player.totalExperience;
            attachment.setExperience((experience / 4) * xp);
            event.setDroppedExperience((int) Math.floor(event.getDroppedExperience() - ((double) event.getDroppedExperience() / 4) * xp));
        }
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
        EPCommands.register(dispatcher);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(level.getBlockEntity(event.getPos()) instanceof GraveBlockEntity grave)) return;
        if (CompatHandler.preventInteraction(player, event.getPos())) return;

        EPGraveDataAttachment data = grave.getData(EPRegistry.GRAVE_DATA);
        String uuid = data.getOwner();
        String timestamp = data.getTimestamp();

        if (!player.getStringUUID().equals(uuid) && !player.hasPermissions(4)) {
            Player owner = level.getPlayerByUUID(UUID.fromString(uuid));
            String user = owner == null ? "Unknown" : owner.getName().getString();
            player.displayClientMessage(EPLanguage.MESSAGE_NO_ACCESS.translate(user).withStyle(ChatFormatting.RED), true);
            return;
        }

        if (BackupHandler.restore(player, timestamp, false) != 0) {
            player.displayClientMessage(EPLanguage.MESSAGE_GRAVE_ERROR.translateFormatted(ChatFormatting.RED), true);
            return;
        }

        for (ItemStack stack : data.getAdditional()) {
            if (stack.isEmpty()) continue;
            if (player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }

        if (grave.getBlockState().getBlock() instanceof GraveBlock block) {
            block.cleanup(level, grave.getBlockPos(), false);
        } else {
            Epitaphs.LOGGER.warn("Grave at '{}' is not a GraveBlock? Make sure this doesnt lead to file spam!", grave.getBlockPos());
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

        player.setData(EPRegistry.LOCATION_DATA, player.getData(EPRegistry.LOCATION_DATA).removeGraveLocation(player, timestamp, grave.getBlockPos()));
        level.removeBlockEntity(event.getPos());
        level.removeBlock(event.getPos(), false);
        event.setCanceled(true);
    }

    private static Instant LAST_BACKUP = Instant.MIN;

    @SubscribeEvent
    private static void scheduleBackup(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        Instant now = Instant.now();
        Duration interval = Duration.ofMinutes(EpitaphsConfig.getBackupInterval());

        if (now.isAfter(LAST_BACKUP.plus(interval))) {
            Epitaphs.LOGGER.info("Running player backup task");
            BackupHandler.saveAll(server);
            LAST_BACKUP = now;
        }
    }
}
