package dev.satherov.epitaphs.datagen;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.datagen.assets.EPBlockStateProvider;
import dev.satherov.epitaphs.datagen.assets.EPItemModelProvider;
import dev.satherov.epitaphs.datagen.assets.EPLanguageProvider;
import dev.satherov.epitaphs.datagen.data.EPBlockTagsProvider;
import dev.satherov.epitaphs.datagen.data.EPEnchantmentTagsProvider;
import dev.satherov.epitaphs.datagen.data.EPItemTagProvider;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.registries.VanillaRegistries;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = Epitaphs.MOD_ID)
public class EPDataGenerator {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {

        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper fileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = CompletableFuture.supplyAsync(VanillaRegistries::createLookup, Util.backgroundExecutor());

        EPDataProvider provider = new EPDataProvider();

        provider.addSubProvider(event.includeClient(), new EPBlockStateProvider(packOutput, fileHelper));
        provider.addSubProvider(event.includeClient(), new EPItemModelProvider(packOutput, fileHelper));
        provider.addSubProvider(event.includeClient(), new EPLanguageProvider(packOutput));

        EPBlockTagsProvider blockTagsProvider = new EPBlockTagsProvider(packOutput, lookupProvider, fileHelper);
        provider.addSubProvider(event.includeServer(), blockTagsProvider);
        provider.addSubProvider(event.includeServer(), new EPItemTagProvider(packOutput, lookupProvider, blockTagsProvider.contentsGetter(), fileHelper));
        provider.addSubProvider(event.includeServer(), new EPEnchantmentTagsProvider(packOutput, lookupProvider, fileHelper));
        provider.addSubProvider(event.includeServer(), new EPDatapackProvider(packOutput, lookupProvider));

        generator.addProvider(true, provider);
    }
}
