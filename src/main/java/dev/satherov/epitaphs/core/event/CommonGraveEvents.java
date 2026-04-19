package dev.satherov.epitaphs.core.event;

import dev.satherov.epitaphs.EPConfig;
import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.client.lang.EPMessageLang;
import dev.satherov.epitaphs.common.block.GraveBlock;
import dev.satherov.epitaphs.common.block.GraveBlockEntity;
import dev.satherov.epitaphs.common.command.EPCommands;
import dev.satherov.epitaphs.common.component.GraveData;
import dev.satherov.epitaphs.common.component.TrackedLocation;
import dev.satherov.epitaphs.common.container.PlayerContainer;
import dev.satherov.epitaphs.common.data.BackupType;
import dev.satherov.epitaphs.common.data.DataHandler;
import dev.satherov.epitaphs.common.data.OnlineHandler;
import dev.satherov.epitaphs.common.data.SoulboundHandler;
import dev.satherov.epitaphs.compat.CuriosHandler;
import dev.satherov.epitaphs.core.EPRegistry;
import dev.satherov.sathlib.network.chat.SLComponent;
import dev.satherov.sathlib.util.SLStringUtils;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
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
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;

import com.mojang.brigadier.CommandDispatcher;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@EventBusSubscriber(modid = Epitaphs.MOD_ID)
public class CommonGraveEvents {
    
    @SubscribeEvent
    public static void onRegisterCommands(final RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        EPCommands.register(dispatcher);
    }
    
    @SubscribeEvent
    public static void onLivingDeath(final LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        ServerLevel level = player.level();
        if (level.getGameRules().get(GameRules.KEEP_INVENTORY)) return;
        
        SoulboundHandler.saveData(player);
        
        Instant now = Instant.now();
        if (DataHandler.save(player, now, BackupType.DEATH) < 1) {
            Epitaphs.log.warn("Failed to save death backup for {}", player.getStringUUID());
            return;
        }
        
        final PlayerContainer container = PlayerContainer.create(player);
        
        if (container.isEmpty()) {
            Epitaphs.log.debug("Player {} has no items on them, skipping grave creation", player.getStringUUID());
            return;
        }
        
        final BlockPos pos = GraveBlock.findSafeSpot(level, player.blockPosition());
        final BlockPos below = pos.below();
        final BlockState state = level.getBlockState(below);
        
        if (state.isAir() || state.is(BlockTags.REPLACEABLE)) {
            level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        }
        
        level.setBlockAndUpdate(pos, EPRegistry.GRAVE.get().defaultBlockState());
        GraveBlockEntity grave = EPRegistry.GRAVE_BLOCK_ENTITY.get().getBlockEntity(level, pos);
        if (grave == null) {
            Epitaphs.log.warn("Failed to create grave block entity at {} for {}", pos, player.getStringUUID());
            return;
        }
        
        grave.setData(EPRegistry.GRAVE_DATA, new GraveData(player, now));
        Epitaphs.log.info("Created grave at {} for {}", pos, player.getStringUUID());
        
        player.sendSystemMessage(EPMessageLang.MESSAGE_GRAVE_CREATED.translate(
                SLComponent.squareBrackets(SLComponent.pos(pos)).style(style -> {
                    style.color(ChatFormatting.GOLD);
                    style.clickEvent(new ClickEvent.SuggestCommand(SLStringUtils.format("/execute in %s run tp @s %s %s %s", level.dimension().identifier(), pos.getX(), pos.getY(), pos.getZ())));
                    style.hoverEvent(new HoverEvent.ShowText(EPMessageLang.MESSAGE_AUTOFILL_COMMAND.translate(ChatFormatting.DARK_GRAY)));
                    return style;
                })
        ).style(ChatFormatting.GREEN), false);
        player.sendSystemMessage(EPMessageLang.MESSAGE_HIGHLIGHT_INFO.translate(
                SLComponent.squareBrackets(Component.literal("/epitaphs highlight latest")).style(style -> {
                    style.color(ChatFormatting.GOLD);
                    style.clickEvent(new ClickEvent.SuggestCommand("/epitaphs highlight latest"));
                    style.hoverEvent(new HoverEvent.ShowText(EPMessageLang.MESSAGE_AUTOFILL_COMMAND.translate(ChatFormatting.DARK_GRAY)));
                    return style;
                })
        ).style(ChatFormatting.GRAY), false);
        
        player.setData(EPRegistry.LOCATION_DATA, player.getData(EPRegistry.LOCATION_DATA).add(now, GlobalPos.of(level.dimension(), pos)));
        
        player.getInventory().clearContent();
        if (CuriosHandler.isLoaded()) CuriosHandler.clearAll(player);
    }
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    private static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SoulboundHandler.restorePlayer(player);
    }
    
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(pos -> {
            final BlockState state = event.getLevel().getBlockState(pos);
            return state.is(EPRegistry.GRAVE.get());
        });
    }
    
    @SubscribeEvent(receiveCanceled = true)
    public static void onRightClickBlock(final PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            if (event.getLevel().getBlockEntity(event.getPos()) instanceof GraveBlockEntity) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(level.getBlockEntity(event.getPos()) instanceof GraveBlockEntity entity)) return;
        
        final MinecraftServer server = level.getServer();
        final BlockPos pos = event.getPos();
        final GraveData data = entity.getData(EPRegistry.GRAVE_DATA);
        final Instant timestamp = data.timestamp();
        final UUID uuid = data.owner();
        final String name = data.name();
        
        if (player.createCommandSourceStack().permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS))) { // Allow operators to open graves that aren't theirs
            
            if (DataHandler.load(player, uuid, timestamp, BackupType.DEATH) < 1) {
                player.sendSystemMessage(EPMessageLang.MESSAGE_RESTORE_FAILED.translate(ChatFormatting.RED), true);
                return;
            }
            
            @Nullable ServerPlayer owner = server.getPlayerList().getPlayer(uuid);
            if (owner != null) owner.setData(EPRegistry.LOCATION_DATA, owner.getData(EPRegistry.LOCATION_DATA).remove(timestamp));
            
        } else {
            
            if (!player.getUUID().equals(uuid)) {
                player.sendSystemMessage(EPMessageLang.MESSAGE_GRAVE_NO_ACCESS.translate(ChatFormatting.RED, name), true);
                return;
            }
            
            if (OnlineHandler.restore(player, timestamp, BackupType.DEATH) < 1) {
                player.sendSystemMessage(EPMessageLang.MESSAGE_RESTORE_FAILED.translate(ChatFormatting.RED), true);
                return;
            }
        }
        
        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS,
                0.4F, ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F
        );
        
        player.setData(EPRegistry.LOCATION_DATA, player.getData(EPRegistry.LOCATION_DATA).remove(timestamp));
        if (player.getData(EPRegistry.TRACKED_LOCATION_DATA).pos().equals(new GlobalPos(level.dimension(), pos))) {
            player.setData(EPRegistry.TRACKED_LOCATION_DATA, TrackedLocation.ZERO);
        }
        
        level.removeBlockEntity(pos);
        level.removeBlock(pos, false);
        Epitaphs.log.debug("Removed grave for {} - {} at {} in {}", name, uuid, pos, level.dimension().identifier());
        
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
    
    private static Instant LAST_BACKUP = Instant.MIN;
    
    @SubscribeEvent
    private static void scheduleBackup(ServerTickEvent.Post event) {
        final MinecraftServer server = event.getServer();
        final Instant now = Instant.now();
        final int minutes = EPConfig.Server.getBackupInterval();
        if (minutes <= 0) return;
        
        if (now.isAfter(CommonGraveEvents.LAST_BACKUP.plus(minutes, ChronoUnit.MINUTES))) {
            Epitaphs.log.info("Running player backup task");
            DataHandler.saveAll(server, now);
            CommonGraveEvents.LAST_BACKUP = now;
        }
    }
}
