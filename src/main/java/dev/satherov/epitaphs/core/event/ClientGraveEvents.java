package dev.satherov.epitaphs.core.event;

import dev.satherov.epitaphs.EPConfig;
import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.client.lang.EPMessageLang;
import dev.satherov.epitaphs.common.block.GraveBlock;
import dev.satherov.epitaphs.common.block.GraveBlockEntity;
import dev.satherov.epitaphs.common.component.GraveData;
import dev.satherov.epitaphs.common.component.TrackedLocation;
import dev.satherov.epitaphs.core.EPRegistry;
import dev.satherov.sathlib.network.chat.SLComponent;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = Epitaphs.MOD_ID, value = Dist.CLIENT)
public final class ClientGraveEvents {
    
    private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
    private static final ContextKey<GraveOutlineState> GRAVE_OUTLINE_STATE = new ContextKey<>(Epitaphs.id("grave_outline_state"));
    
    @SubscribeEvent
    public static void onExtractLevelRenderState(final ExtractLevelRenderStateEvent event) {
        final Minecraft mc = Minecraft.getInstance();
        final LocalPlayer player = mc.player;
        final ClientLevel level = event.getLevel();
        if (player == null) {
            event.getRenderState().setRenderData(ClientGraveEvents.GRAVE_OUTLINE_STATE, null);
            return;
        }
        
        final TrackedLocation tracked = player.getData(EPRegistry.TRACKED_LOCATION_DATA);
        if (tracked.isEmpty()) {
            event.getRenderState().setRenderData(ClientGraveEvents.GRAVE_OUTLINE_STATE, null);
            return;
        }
        
        final GlobalPos target = tracked.pos();
        if (!level.dimension().equals(target.dimension())) {
            event.getRenderState().setRenderData(ClientGraveEvents.GRAVE_OUTLINE_STATE, null);
            return;
        }
        
        final BlockPos pos = target.pos();
        if (!level.isLoaded(pos)) {
            event.getRenderState().setRenderData(ClientGraveEvents.GRAVE_OUTLINE_STATE, null);
            return;
        }
        
        final var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof GraveBlock)) {
            event.getRenderState().setRenderData(ClientGraveEvents.GRAVE_OUTLINE_STATE, null);
            return;
        }
        
        final BlockModelRenderState model = new BlockModelRenderState();
        mc.getBlockModelResolver().update(model, state, ClientGraveEvents.BLOCK_DISPLAY_CONTEXT);
        if (model.isEmpty()) {
            event.getRenderState().setRenderData(ClientGraveEvents.GRAVE_OUTLINE_STATE, null);
            return;
        }
        
        event.getRenderState().setRenderData(
                ClientGraveEvents.GRAVE_OUTLINE_STATE,
                new GraveOutlineState(pos.immutable(), model, LevelRenderer.getLightCoords(level, pos))
        );
        event.getRenderState().haveGlowingEntities = true;
    }
    
    @SubscribeEvent
    public static void onSubmitCustomGeometry(final SubmitCustomGeometryEvent event) {
        final GraveOutlineState outline = event.getLevelRenderState().getRenderData(ClientGraveEvents.GRAVE_OUTLINE_STATE);
        if (outline == null) return;
        
        final BlockPos pos = outline.pos();
        final var cameraPos = event.getLevelRenderState().cameraRenderState.pos;
        
        event.getPoseStack().pushPose();
        event.getPoseStack().translate(
                pos.getX() - cameraPos.x,
                pos.getY() - cameraPos.y,
                pos.getZ() - cameraPos.z
        );
        outline.model().submitOnlyOutline(
                event.getPoseStack(),
                event.getSubmitNodeCollector(),
                outline.lightCoords(),
                OverlayTexture.NO_OVERLAY,
                0
        );
        event.getPoseStack().popPose();
    }
    
    @SubscribeEvent
    public static void onRenderGuiOverlay(final RenderGuiEvent.Post event) {
        final Minecraft mc = Minecraft.getInstance();
        final LocalPlayer player = mc.player;
        final ClientLevel level = mc.level;
        if ((mc.screen != null && !(mc.screen instanceof ChatScreen)) || player == null || level == null) return;
        
        final GuiGraphicsExtractor graphics = event.getGuiGraphics();
        final TrackedLocation tracked = player.getData(EPRegistry.TRACKED_LOCATION_DATA);
        if (!tracked.isEmpty()) {
            graphics.nextStratum();
            graphics.pose().pushMatrix();
            int yShift = Math.max(mc.gui.leftHeight, mc.gui.rightHeight);
            graphics.pose().translate((float) graphics.guiWidth() / 2, graphics.guiHeight() - Math.max(yShift, 52));
            
            Component message = ClientGraveEvents.distanceMessage(player, level, tracked.pos());
            int width = mc.font.width(message);
            graphics.textWithBackdrop(mc.font, message, -width / 2, -4, width, 0xFFFFFFFF);
            graphics.pose().popMatrix();
        }
        
        final HitResult result = mc.hitResult;
        if (!(result instanceof BlockHitResult hit)) return;
        
        final BlockPos pos = hit.getBlockPos();
        final var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof GraveBlock)) return;
        if (!(level.getBlockEntity(pos) instanceof GraveBlockEntity grave)) return;
        
        graphics.nextStratum();
        graphics.tooltip(
                mc.font,
                ClientGraveEvents.createTooltip(player, grave.getData(EPRegistry.GRAVE_DATA)),
                graphics.guiWidth() / 2 + 3,
                graphics.guiHeight() / 2,
                DefaultTooltipPositioner.INSTANCE,
                null
        );
    }
    
    private static Component distanceMessage(LocalPlayer player, ClientLevel level, GlobalPos target) {
        final Component argument;
        if (!level.dimension().equals(target.dimension())) argument = Component.literal(target.dimension().identifier().toString());
        else argument = SLComponent.pos(target.pos().subtract(player.blockPosition()));
        
        return EPMessageLang.MESSAGE_DISTANCE_TO_GRAVE.translate(SLComponent.squareBrackets(argument).style(ChatFormatting.GOLD));
    }
    
    private static List<ClientTooltipComponent> createTooltip(LocalPlayer player, GraveData data) {
        final String name = data.name();
        final UUID uuid = data.owner();
        final String timestamp = EPConfig.Client.getTooltipFormatter().formatter().format(data.timestamp());
        
        final List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(name + " - " + timestamp).withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal(uuid.toString()).withStyle(ChatFormatting.DARK_GRAY));
        if (!player.getUUID().equals(uuid)) {
            if (player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS))) {
                lines.add(Component.translatable(EPMessageLang.MESSAGE_GRAVE_OP_BYPASS.key()).withStyle(ChatFormatting.GREEN));
            } else {
                lines.add(Component.translatable(EPMessageLang.MESSAGE_GRAVE_NO_ACCESS.key(), name).withStyle(ChatFormatting.RED));
            }
        }
        
        return lines.stream()
                .map(Component::getVisualOrderText)
                .map(ClientTooltipComponent::create)
                .toList();
    }
    
    private record GraveOutlineState(BlockPos pos, BlockModelRenderState model, int lightCoords) { }
}
