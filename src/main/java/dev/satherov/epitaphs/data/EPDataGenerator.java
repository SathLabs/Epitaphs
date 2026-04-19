package dev.satherov.epitaphs.data;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.data.pack.EPEnchantments;
import dev.satherov.epitaphs.data.provider.EPLanguageProvider;
import dev.satherov.epitaphs.data.provider.EPModelProvider;
import dev.satherov.epitaphs.data.provider.tags.EPBlockTagsProvider;
import dev.satherov.epitaphs.data.provider.tags.EPEnchantmentTagsProvider;
import dev.satherov.epitaphs.data.provider.tags.EPItemTagProvider;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;

@EventBusSubscriber(modid = Epitaphs.MOD_ID)
public class EPDataGenerator {
    
    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.ENCHANTMENT, EPEnchantments::bootstrap);
    
    
    @SubscribeEvent
    private static void onGatherData(GatherDataEvent.Client event) {
        
        event.createProvider(EPLanguageProvider::new);
        event.createProvider(EPModelProvider::new);
        
        event.createProvider(EPBlockTagsProvider::new);
        event.createProvider(EPItemTagProvider::new);
        event.createProvider(EPEnchantmentTagsProvider::new);
        
        event.createDatapackRegistryObjects(EPDataGenerator.BUILDER);
    }
}

