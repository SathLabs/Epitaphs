package dev.satherov.epitaphs.core.event;

import lombok.experimental.UtilityClass;

import dev.satherov.epitaphs.EPConfig;
import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.client.lang.EPLanguage;
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
import dev.satherov.epitaphs.compat.AccessoriesHandler;
import dev.satherov.epitaphs.compat.CuriosHandler;
import dev.satherov.epitaphs.core.EPRegistry;
import dev.satherov.epitaphs.util.StringUtils;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@UtilityClass
public class GraveEvents {
    
    @EventBusSubscriber(modid = Epitaphs.MOD_ID, value = Dist.CLIENT)
    public static class Client {
        
        @SubscribeEvent
        public static void onRenderLevelStage(final RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
            
            final Minecraft mc = Minecraft.getInstance();
            final LocalPlayer player = mc.player;
            final ClientLevel level = mc.level;
            if (player == null || level == null) return;
            
            TrackedLocation tracked = player.getData(EPRegistry.TRACKED_LOCATION_DATA);
            if (tracked.isEmpty()) return;
            
            GlobalPos target = tracked.pos();
            if (!level.dimension().equals(target.dimension())) {
                mc.gui.setOverlayMessage(EPLanguage.MESSAGE_DISTANCE_TO_GRAVE.text(
                        ComponentUtils.wrapInSquareBrackets(Component.literal(target.dimension().location().toString())).withStyle(ChatFormatting.GOLD)
                ), false);
                return;
            }
            
            final BlockPos pos = target.pos();
            final BlockPos current = player.blockPosition();
            final BlockPos delta = new BlockPos(
                    pos.getX() - current.getX(),
                    pos.getY() - current.getY(),
                    pos.getZ() - current.getZ()
            );
            mc.gui.setOverlayMessage(EPLanguage.MESSAGE_DISTANCE_TO_GRAVE.text(
                    ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", delta.getX(), delta.getY(), delta.getZ())).withStyle(ChatFormatting.GOLD)
            ), false);
            if (!level.isLoaded(pos)) return;
            
            final BlockState state = level.getBlockState(pos);
            if (!state.is(EPRegistry.GRAVE.get())) return;
            
            final OutlineBufferSource outline = mc.renderBuffers().outlineBufferSource();
            outline.setColor(255, 255, 255, 255);
            mc.levelRenderer.requestOutlineEffect();
            
            event.getPoseStack().pushPose();
            event.getPoseStack().translate(
                    pos.getX() - event.getCamera().getPosition().x,
                    pos.getY() - event.getCamera().getPosition().y,
                    pos.getZ() - event.getCamera().getPosition().z
            );
            
            mc.getBlockRenderer().renderSingleBlock(
                    state,
                    event.getPoseStack(),
                    outline,
                    0x00F000F0,
                    OverlayTexture.NO_OVERLAY,
                    ModelData.EMPTY,
                    RenderType.outline(InventoryMenu.BLOCK_ATLAS)
            );
            
            event.getPoseStack().popPose();
        }
        
        @SubscribeEvent
        public static void onRenderGuiOverlay(final RenderGuiEvent.Pre event) {
            final Minecraft mc = Minecraft.getInstance();
            final HitResult result = mc.hitResult;
            if (!(result instanceof BlockHitResult hit)) return;
            
            final LocalPlayer player = mc.player;
            final ClientLevel level = mc.level;
            if (player == null || level == null) return;
            
            final BlockPos pos = hit.getBlockPos();
            final BlockState state = level.getBlockState(pos);
            final BlockEntity entity = EPRegistry.GRAVE_BLOCK_ENTITY.get().getBlockEntity(level, pos);
            if (!(state.getBlock() instanceof GraveBlock && entity != null)) return;
            
            final GraveData data = entity.getData(EPRegistry.GRAVE_DATA);
            final String name = data.name();
            final UUID uuid = data.owner();
            final String timestamp = EPConfig.Client.getTooltipFormatter().formatter().format(data.timestamp());
            
            List<Component> elements = new ArrayList<>();
            elements.add(Component.literal(StringUtils.format("%s - %s", name, timestamp)).withStyle(ChatFormatting.GRAY));
            elements.add(Component.literal(uuid.toString()).withStyle(ChatFormatting.DARK_GRAY));
            if (!player.getUUID().equals(uuid)) {
                if (player.hasPermissions(2)) elements.add(EPLanguage.MESSAGE_GRAVE_OP_BYPASS.text(ChatFormatting.GREEN));
                else elements.add(EPLanguage.MESSAGE_GRAVE_NO_ACCESS.text(name).withStyle(ChatFormatting.RED));
            }
            
            final GuiGraphics graphics = event.getGuiGraphics();
            final int x = graphics.guiWidth() / 2;
            final int y = graphics.guiHeight() / 2;
            
            graphics.renderTooltip(mc.font, elements, Optional.empty(), x, y);
        }
    }
    
    @EventBusSubscriber(modid = Epitaphs.MOD_ID)
    public static class Common {
        
        @SubscribeEvent
        public static void onRegisterCommands(final RegisterCommandsEvent event) {
            CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
            EPCommands.register(dispatcher);
        }
        
        @SubscribeEvent
        public static void onLivingDeath(final LivingDeathEvent event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            
            ServerLevel level = player.serverLevel();
            if (level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).get()) return;
            
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
            
            final GameProfile profile = player.getGameProfile();
            grave.setData(EPRegistry.GRAVE_DATA, new GraveData(profile.getId(), now, profile.getName()));
            Epitaphs.log.info("Created grave at {} for {}", pos, player.getStringUUID());
            
            player.displayClientMessage(EPLanguage.MESSAGE_GRAVE_CREATED.text(
                    ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", pos.getX(), pos.getY(), pos.getZ())).withStyle(style -> {
                        style = style.withColor(ChatFormatting.GOLD);
                        style = style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, StringUtils.format("/execute in %s run tp @s %s %s %s", level.dimension().location(), pos.getX(), pos.getY(), pos.getZ())));
                        style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, EPLanguage.MESSAGE_AUTOFILL_COMMAND.text(ChatFormatting.DARK_GRAY)));
                        return style;
                    })
            ).withStyle(ChatFormatting.GREEN), false);
            player.displayClientMessage(EPLanguage.MESSAGE_HIGHLIGHT_INFO.text(
                    ComponentUtils.wrapInSquareBrackets(Component.literal("/epitaphs highlight latest")).withStyle(style -> {
                        style = style.withColor(ChatFormatting.GOLD);
                        style = style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/epitaphs highlight latest"));
                        style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, EPLanguage.MESSAGE_AUTOFILL_COMMAND.text(ChatFormatting.DARK_GRAY)));
                        return style;
                    })
            ).withStyle(ChatFormatting.GRAY), false);
            
            player.setData(EPRegistry.LOCATION_DATA, player.getData(EPRegistry.LOCATION_DATA).add(now, GlobalPos.of(level.dimension(), pos)));
            
            player.getInventory().clearContent();
            if (CuriosHandler.isLoaded()) CuriosHandler.clearAll(player);
            if (AccessoriesHandler.isLoaded()) AccessoriesHandler.clearAll(player);
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
            
            if (player.hasPermissions(2)) { // Allow operators to open graves that aren't theirs
                
                if (DataHandler.load(player, uuid, timestamp, BackupType.DEATH) < 1) {
                    player.displayClientMessage(EPLanguage.MESSAGE_RESTORE_FAILED.text(ChatFormatting.RED), true);
                    return;
                }
                
                @Nullable ServerPlayer owner = server.getPlayerList().getPlayer(uuid);
                if (owner != null) owner.setData(EPRegistry.LOCATION_DATA, owner.getData(EPRegistry.LOCATION_DATA).remove(timestamp));
                
            } else {
                
                if (!player.getUUID().equals(uuid)) {
                    player.displayClientMessage(EPLanguage.MESSAGE_GRAVE_NO_ACCESS.text(ChatFormatting.RED, name), true);
                    return;
                }
                
                if (OnlineHandler.restore(player, timestamp, BackupType.DEATH) < 1) {
                    player.displayClientMessage(EPLanguage.MESSAGE_RESTORE_FAILED.text(ChatFormatting.RED), true);
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
            DataHandler.invalidate(server, uuid, timestamp);
            Epitaphs.log.debug("Removed grave for {} - {} at {} in {}", name, uuid, pos, level.dimension().location());
            
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
            
            if (now.isAfter(Common.LAST_BACKUP.plus(minutes, ChronoUnit.MINUTES))) {
                Epitaphs.log.info("Running player backup task");
                DataHandler.saveAll(server, now);
                Common.LAST_BACKUP = now;
            }
        }
    }
}
