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
import dev.satherov.epitaphs.common.item.SoulBottleItem;
import dev.satherov.epitaphs.compat.CuriosHandler;
import dev.satherov.epitaphs.core.EPRegistry;
import dev.satherov.epitaphs.data.pack.EPEnchantments;
import dev.satherov.sathlib.network.chat.SLComponent;
import dev.satherov.sathlib.util.SLStringUtils;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
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
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BottleItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@EventBusSubscriber(modid = Epitaphs.MOD_ID)
public class CommonGraveEvents {
    
    private static Instant LAST_BACKUP = Instant.MIN;
    
    @SubscribeEvent
    public static void onRegisterCommands(final RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        EPCommands.register(dispatcher);
    }
    
    @SubscribeEvent
    public static void onLivingDeath(final LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        final GameProfile profile = player.getGameProfile();
        
        Epitaphs.log.debug("Player {} was killed by {}", profile.name(), event.getSource());
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            final ItemStack main = killer.getMainHandItem();
            final DataComponentMap mainComponents = main.getComponents();
            if (!main.isEmpty()) Epitaphs.log.debug("Killer main-hand: {}[{}]", main, mainComponents.isEmpty() ? "empty" : mainComponents.toString());
            
            final ItemStack off = killer.getOffhandItem();
            final DataComponentMap offComponents = off.getComponents();
            if (!off.isEmpty()) Epitaphs.log.debug("Killer off-hand: {}[{}]", off, offComponents.isEmpty() ? "empty" : offComponents.toString());
        }
        
        if (event.isCanceled()) {
            Epitaphs.log.warn("LivingDeathEvent for {} was canceled by another mod, this will almost certainly cause issues!", profile.name());
            return;
        }
        
        if (player.getPersistentData().getBoolean("epitaphs:player_is_dead").orElse(false)) {
            Epitaphs.log.warn("LivingDeathEvent for {} was called a second time while the player is already dead, this is almost certainly unintended", profile.name());
            return;
        }
        
        player.getPersistentData().putBoolean("epitaphs:player_is_dead", true);
        ServerLevel level = player.level();
        if (level.getGameRules().get(GameRules.KEEP_INVENTORY)) {
            Epitaphs.log.debug("KeepInventory is enabled, skipping grave creation for {}", profile.name());
            return;
        }
        
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
        Epitaphs.log.debug("Found safe grave spot for {} at {}", profile.name(), pos);
        
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
        player.getPersistentData().putBoolean("epitaphs:player_is_dead", false);
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
        
        final MinecraftServer server = level.getServer();
        final BlockPos pos = event.getPos();
        final BlockState state = level.getBlockState(pos);
        
        if (!state.is(EPRegistry.GRAVE.get())) return;
        if (!(level.getBlockEntity(pos) instanceof GraveBlockEntity entity)) return;
        
        final ItemStack stack = player.getMainHandItem();
        final GraveData data = entity.getData(EPRegistry.GRAVE_DATA);
        final Instant timestamp = data.timestamp();
        final UUID uuid = data.owner();
        final String name = data.name();
        
        if (state.getValue(GraveBlock.SOULS) && stack.getItem() instanceof BottleItem) {
            level.setBlockAndUpdate(pos, state.setValue(GraveBlock.SOULS, false));
            stack.shrink(1);
            final ItemStack bottle = EPRegistry.SOUL_BOTTLE.get().getDefaultInstance();
            
            if (stack.isEmpty()) player.setItemSlot(EquipmentSlot.MAINHAND, bottle);
            else if (!player.getInventory().add(bottle)) player.drop(bottle, true);
            level.playSound(null, pos, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.BLOCKS, 1.0F, 1.0F);
            return;
        }
        
        if (player.createCommandSourceStack().permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS))) { // Allow operators to open graves that aren't theirs
            
            if (DataHandler.load(player, uuid, timestamp, BackupType.DEATH) < 1) {
                player.sendSystemMessage(EPMessageLang.MESSAGE_RESTORE_FAILED.translate(ChatFormatting.RED), true);
                return;
            }
            
            @Nullable ServerPlayer owner = server.getPlayerList().getPlayer(uuid);
            if (owner != null) {
                owner.setData(EPRegistry.LOCATION_DATA, owner.getData(EPRegistry.LOCATION_DATA).remove(timestamp));
                Epitaphs.log.debug("Removed grave location for {} at {}", owner.getGameProfile().name(), pos);
            }
            
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
        DataHandler.invalidate(server, uuid, timestamp);
        Epitaphs.log.debug("Removed grave for {} - {} at {} in {}", name, uuid, pos, level.dimension().identifier());
        
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
    
    @SubscribeEvent
    public static void onAnvilCraftUpdated(final AnvilUpdateEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        final RegistryAccess access = player.registryAccess();
        final HolderGetter<Enchantment> lookup = access.lookupOrThrow(Registries.ENCHANTMENT);
        
        final ItemStack right = event.getRight();
        final ItemStack left = event.getLeft();
        
        final boolean soulboundValid = left.is(EPRegistry.SOULBOUND_ENCHANTABLE) && !EnchantmentHelper.has(left, EPRegistry.EXPERIENCE_SOULBOUND.get()) && right.getItem() instanceof SoulBottleItem;
        final boolean experienceSoulboundValid = left.is(EPRegistry.EXPERIENCE_SOULBOUND_ENCHANTABLE) && EnchantmentHelper.has(left, EPRegistry.SOULBOUND.get()) && right.getItem() instanceof ExperienceBottleItem;
        
        if (!(soulboundValid || experienceSoulboundValid)) return;
        
        final Holder<Enchantment> soulbound = lookup.getOrThrow(EPEnchantments.SOULBOUND);
        final Holder<Enchantment> experienceSoulbound = lookup.getOrThrow(EPEnchantments.EXPERIENCE_SOULBOUND);
        
        final ItemStack result = left.copy();
        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(result));
        
        if (experienceSoulboundValid) {
            for (Holder<Enchantment> entry : enchantments.keySet()) {
                if (!Enchantment.areCompatible(experienceSoulbound, entry)) return;
            }
            enchantments.set(experienceSoulbound, 1);
            enchantments.removeIf(x -> x.equals(soulbound));
        } else {
            for (Holder<Enchantment> entry : enchantments.keySet()) {
                if (!Enchantment.areCompatible(soulbound, entry)) return;
            }
            enchantments.set(soulbound, 1);
        }
        
        EnchantmentHelper.setEnchantments(result, enchantments.toImmutable());
        event.setMaterialCost(1);
        event.setXpCost(1);
        event.setOutput(result);
    }
    
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
