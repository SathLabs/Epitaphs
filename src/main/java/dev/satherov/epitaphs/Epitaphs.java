package dev.satherov.epitaphs;

import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;

import dev.satherov.epitaphs.config.ConfigLoader;
import dev.satherov.epitaphs.core.EPRegistry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLModContainer;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

@Slf4j(access = AccessLevel.PUBLIC)
@Mod(Epitaphs.MOD_ID)
public class Epitaphs {
    
    public static final String MOD_ID = "epitaphs";
    
    public Epitaphs(IEventBus bus, FMLModContainer container) {
        ConfigLoader.discover(container);
        EPRegistry.register(bus);
        
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (FMLLoader.isProduction()) return;
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            server.getPlayerList().op(event.getEntity().getGameProfile());
        });
    }
    
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Epitaphs.MOD_ID, path);
    }
    
    public static ResourceLocation neo(String path) {
        return ResourceLocation.fromNamespaceAndPath("c", path);
    }
}
