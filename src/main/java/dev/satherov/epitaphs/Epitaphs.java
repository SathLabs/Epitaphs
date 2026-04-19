package dev.satherov.epitaphs;

import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;

import dev.satherov.epitaphs.core.EPRegistry;
import dev.satherov.sathlib.config.SLConfigLoader;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLModContainer;

import net.minecraft.resources.Identifier;

@Slf4j(access = AccessLevel.PUBLIC)
@Mod(Epitaphs.MOD_ID)
public class Epitaphs {
    
    public static final String MOD_ID = "epitaphs";
    
    public Epitaphs(final IEventBus bus, final FMLModContainer container) {
        SLConfigLoader.discover(container);
        EPRegistry.register(bus);
    }
    
    public static Identifier id(final String path) {
        return Identifier.fromNamespaceAndPath(Epitaphs.MOD_ID, path);
    }
    
    public static Identifier neo(final String path) {
        return Identifier.fromNamespaceAndPath("c", path);
    }
}
