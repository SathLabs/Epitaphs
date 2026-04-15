package dev.satherov.epitaphs.data.provider.tags;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.data.pack.EPEnchantments;

import net.neoforged.neoforge.common.data.ExistingFileHelper;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EnchantmentTagsProvider;
import net.minecraft.tags.EnchantmentTags;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class EPEnchantmentTagsProvider extends EnchantmentTagsProvider {
    
    
    public EPEnchantmentTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, @Nullable ExistingFileHelper fileHelper) {
        super(output, lookup, Epitaphs.MOD_ID, fileHelper);
    }
    
    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        
        this.tag(EnchantmentTags.NON_TREASURE)
                .addOptional(EPEnchantments.SOULBOUND.location())
                .addOptional(EPEnchantments.EXPERIENCE_SOULBOUND.location());
    }
}
