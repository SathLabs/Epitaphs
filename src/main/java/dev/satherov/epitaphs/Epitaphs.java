package dev.satherov.epitaphs;

import dev.satherov.epitaphs.common.tile.EPGraveBlockEntity;
import dev.satherov.epitaphs.core.EPRegistry;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;

@Mod(Epitaphs.MOD_ID)
public class Epitaphs {

    public static final String MOD_ID = "epitaphs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Epitaphs(IEventBus modEventBus, ModContainer modContainer) {

        EPRegistry.BLOCKS.register(modEventBus);
        EPRegistry.ITEMS.register(modEventBus);
        EPRegistry.BLOCK_ENTITY_TYPES.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(Epitaphs::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(Epitaphs::onRightClickBlock);
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();

            if (player.getInventory().isEmpty()) return;

            level.setBlockAndUpdate(player.blockPosition(), EPRegistry.GRAVE.get().defaultBlockState());

            EPGraveBlockEntity grave = new EPGraveBlockEntity(player.blockPosition(), level.getBlockState(player.blockPosition()));
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i).copy();
                if (!stack.isEmpty()) {
                    grave.setItem(i, stack);
                }
            }
            level.setBlockEntity(grave);
            player.getInventory().clearContent();
        }
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel() instanceof ServerLevel level) {
            if (level.getBlockEntity(event.getPos()) instanceof EPGraveBlockEntity grave) {
                ServerPlayer player = (ServerPlayer) event.getEntity();
                List<ItemStack> overflow = new ArrayList<>();

                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = grave.removeItem(i);
                    if (stack.isEmpty()) continue;
                    if (!player.getInventory().getItem(i).isEmpty()) {
                        overflow.add(stack);
                        continue;
                    }
                    player.getInventory().setItem(i, stack);
                }

                for (ItemStack stack : overflow) {
                    if(!player.getInventory().add(stack)) {
                        ItemEntity drop = player.drop(stack, false);
                        if(drop != null) drop.setNoPickUpDelay();
                    }
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
                        0.2F,
                        ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F
                );

                level.removeBlockEntity(event.getPos());
                level.removeBlock(event.getPos(), false);
                event.setCanceled(true);
            }
        }
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}