package dev.satherov.epitaphs.compat;

import dev.satherov.epitaphs.core.EPRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

import dev.ftb.mods.ftbchunks.api.Protection;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;

public class FTBChunksHandler {

    private static final ClaimedChunkManagerImpl INSTANCE = ClaimedChunkManagerImpl.getInstance();

    public static boolean preventInteractions(ServerPlayer player, BlockPos pos) {
        if (player.serverLevel().getBlockState(pos).is(EPRegistry.GRAVE)) return false;
        return INSTANCE.shouldPreventInteraction(player, InteractionHand.MAIN_HAND, pos, Protection.INTERACT_BLOCK, null) ||
                INSTANCE.shouldPreventInteraction(player, InteractionHand.OFF_HAND, pos, Protection.INTERACT_BLOCK, null);
    }
}
