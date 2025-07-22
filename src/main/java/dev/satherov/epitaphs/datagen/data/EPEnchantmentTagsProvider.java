package dev.satherov.epitaphs.datagen.data;

import dev.satherov.epitaphs.Epitaphs;

import net.neoforged.neoforge.common.data.ExistingFileHelper;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EnchantmentTagsProvider;
import net.minecraft.tags.EnchantmentTags;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class EPEnchantmentTagsProvider extends EnchantmentTagsProvider {


    public EPEnchantmentTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Epitaphs.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {

        tag(EnchantmentTags.NON_TREASURE)
                .addOptional(EPEnchantments.SOULBOUND.location())
                .addOptional(EPEnchantments.EXPERIENCE_SOULBOUND.location());
    }
}
