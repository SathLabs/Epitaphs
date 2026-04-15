package dev.satherov.epitaphs.data;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.data.pack.EPEnchantments;
import dev.satherov.epitaphs.data.provider.EPBlockStateProvider;
import dev.satherov.epitaphs.data.provider.EPLanguageProvider;
import dev.satherov.epitaphs.data.provider.tags.EPBlockTagsProvider;
import dev.satherov.epitaphs.data.provider.tags.EPEnchantmentTagsProvider;
import dev.satherov.epitaphs.data.provider.tags.EPItemTagProvider;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import net.minecraft.core.registries.Registries;

@EventBusSubscriber(modid = Epitaphs.MOD_ID)
public class EPDataGenerator {
    
    @SubscribeEvent
    private static void onGatherData(GatherDataEvent event) {
        EPDataProvider provider = EPDataProvider.create(event);
        
        provider.add(EPLanguageProvider::new);
        provider.add(EPItemTagProvider::new);
        provider.add(EPBlockStateProvider::new);
        
        provider.add(EPBlockTagsProvider::new);
        provider.add(EPItemTagProvider::new);
        provider.add(EPEnchantmentTagsProvider::new);
        
        provider.add(Registries.ENCHANTMENT, EPEnchantments::bootstrap);
        
        provider.generate();
    }
}

