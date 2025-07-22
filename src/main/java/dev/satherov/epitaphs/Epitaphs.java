package dev.satherov.epitaphs;

import dev.satherov.epitaphs.core.EPRegistry;
import dev.satherov.epitaphs.core.EPEventManager;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Epitaphs.MOD_ID)
public class Epitaphs {

    public static final String MOD_ID = "epitaphs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Epitaphs(IEventBus modEventBus, ModContainer modContainer) {

        EPRegistry.ENCHANTMENT_DATA_COMPONENTS.register(modEventBus);
        EPRegistry.ATTACHMENT_TYPES.register(modEventBus);
        EPRegistry.BLOCKS.register(modEventBus);
        EPRegistry.ITEMS.register(modEventBus);
        EPRegistry.BLOCK_ENTITY_TYPES.register(modEventBus);

        NeoForge.EVENT_BUS.register(EPEventManager.class);

        modContainer.registerConfig(ModConfig.Type.SERVER, EpitaphsConfig.Server.SPEC);
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static ResourceLocation neo(String path) {
        return ResourceLocation.fromNamespaceAndPath("c", path);
    }
}