package dev.satherov.epitaphs.common.data;

import lombok.experimental.UtilityClass;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.common.component.SoulboundData;
import dev.satherov.epitaphs.common.container.AccessoriesContainer;
import dev.satherov.epitaphs.common.container.CuriosContainer;
import dev.satherov.epitaphs.common.container.InventoryContainer;
import dev.satherov.epitaphs.common.container.PlayerContainer;
import dev.satherov.epitaphs.compat.AccessoriesHandler;
import dev.satherov.epitaphs.compat.CuriosHandler;
import dev.satherov.epitaphs.core.EPRegistry;
import dev.satherov.epitaphs.util.MathUtils;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import com.mojang.authlib.GameProfile;

import java.util.UUID;

@UtilityClass
public class SoulboundHandler {
    
    ///
    /// Writes the soulbound data for the given player to be restored after respawn
    ///
    /// @param player the player to write the soulbound data for
    ///
    public static void saveData(ServerPlayer player) {
        if (!player.getData(EPRegistry.SOULBOUND_DATA).isEmpty()) {
            Epitaphs.log.warn("Soulbound data has already been saved. How did we get here...");
            return;
        }
        
        final UUID uuid = player.getUUID();
        final int experience = SoulboundHandler.extractExperience(player);
        final InventoryContainer inventory = InventoryContainer.createSoulbound(player);
        final CuriosContainer curios = CuriosHandler.isLoaded() ? CuriosContainer.createSoulbound(player) : CuriosContainer.empty();
        final AccessoriesContainer accessories = AccessoriesHandler.isLoaded() ? AccessoriesContainer.createSoulbound(player) : AccessoriesContainer.empty();
        final PlayerContainer container = new PlayerContainer(uuid, inventory, curios, accessories);
        
        final SoulboundData data = new SoulboundData(container, experience);
        player.setData(EPRegistry.SOULBOUND_DATA, data);
        Epitaphs.log.debug("Stored Soulbound data for {}", player.getGameProfile().getName());
    }
    
    ///
    /// Extracts the experience points to be saved for the given Player.
    ///
    /// Counts the number of armor pieces with the xp bound enchantment and then splits
    /// the total experience into 4 equal parts, recombining n parts for n armor pieces
    /// with the enchantment
    ///
    /// @param player the player to extract experience from
    ///
    /// @return amount of experience to save
    ///
    private int extractExperience(ServerPlayer player) {
        final int experience = player.totalExperience;
        final Inventory inventory = player.getInventory();
        final NonNullList<ItemStack> armor = inventory.armor;
        
        // Count the number of xp bound armor pieces
        int count = 0;
        for (ItemStack stack : armor) if (SoulboundHandler.isXPBound(stack)) count++;
        
        // Split the total experiences into 4 pieces
        final int[] parts = MathUtils.split(experience, 4);
        int result = 0;
        
        // recombine n parts of the total xp for n armor pieces with the xp bound enchantment
        for (int index = 0; index < count; index++) {
            int part = parts[index];
            result += part;
            player.totalExperience -= part;
        }
        
        Epitaphs.log.debug("Saved {} experience points out of {} total experience points for soulbound data of {}", result, experience, player.getGameProfile().getName());
        return result;
    }
    
    ///
    /// Restores the player from the currently save {@link EPRegistry#SOULBOUND_DATA}.
    /// Does nothing if the data is empty
    ///
    /// @param player the Player to restore
    ///
    public static void restorePlayer(ServerPlayer player) {
        final GameProfile profile = player.getGameProfile();
        final SoulboundData data = player.getData(EPRegistry.SOULBOUND_DATA);
        if (data.isEmpty()) return;
        
        final int experience = data.experience();
        final PlayerContainer container = data.container();
        
        player.giveExperiencePoints(experience);
        Epitaphs.log.debug("Restored {} experience points for {}", experience, profile.getName());
        
        container.write(player);
        
        player.removeData(EPRegistry.SOULBOUND_DATA);
        Epitaphs.log.debug("Restored soulbound data for {}", profile.getName());
    }
    
    ///
    /// Checks if the given {@link ItemStack} has either the {@link EPRegistry#SOULBOUND} or {@link EPRegistry#EXPERIENCE_SOULBOUND} enchantment.
    ///
    /// @param stack {@link ItemStack} to check.
    ///
    /// @return {@code true} if the {@link ItemStack} has either the {@link EPRegistry#SOULBOUND} or {@link EPRegistry#EXPERIENCE_SOULBOUND} enchantment.
    ///
    public static boolean isSoulbound(ItemStack stack) {
        return EnchantmentHelper.has(stack, EPRegistry.SOULBOUND.get()) || EnchantmentHelper.has(stack, EPRegistry.EXPERIENCE_SOULBOUND.get());
    }
    
    ///
    /// Checks if the given {@link ItemStack} has the {@link EPRegistry#SOULBOUND} enchantment.
    ///
    /// @param stack {@link ItemStack} to check.
    ///
    /// @return {@code true} if the {@link ItemStack} has the {@link EPRegistry#SOULBOUND} enchantment.
    ///
    public static boolean isXPBound(ItemStack stack) {
        return EnchantmentHelper.has(stack, EPRegistry.EXPERIENCE_SOULBOUND.get());
    }
}
