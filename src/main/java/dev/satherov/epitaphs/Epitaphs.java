package dev.satherov.epitaphs;

import dev.satherov.epitaphs.core.EPRegistry;
import dev.satherov.epitaphs.core.EPEventManager;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Epitaphs.MOD_ID)
public class Epitaphs {

    public static final String MOD_ID = "epitaphs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Epitaphs(IEventBus modEventBus, ModContainer modContainer) {

        EPRegistry.BLOCKS.register(modEventBus);
        EPRegistry.ITEMS.register(modEventBus);
        EPRegistry.BLOCK_ENTITY_TYPES.register(modEventBus);

        NeoForge.EVENT_BUS.register(EPEventManager.class);
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}