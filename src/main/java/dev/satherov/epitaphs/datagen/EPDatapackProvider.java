package dev.satherov.epitaphs.datagen;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.datagen.data.EPEnchantments;

import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class EPDatapackProvider extends DatapackBuiltinEntriesProvider {
    
    private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.ENCHANTMENT, EPEnchantments::bootstrap);
    
    public EPDatapackProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(Epitaphs.MOD_ID));
    }
}
