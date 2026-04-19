package dev.satherov.epitaphs.data.provider.tags;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.data.pack.EPEnchantments;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EnchantmentTagsProvider;
import net.minecraft.tags.EnchantmentTags;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class EPEnchantmentTagsProvider extends EnchantmentTagsProvider {
    
    
    public EPEnchantmentTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, Epitaphs.MOD_ID);
    }
    
    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        
        this.tag(EnchantmentTags.NON_TREASURE)
                .addOptional(EPEnchantments.SOULBOUND)
                .addOptional(EPEnchantments.EXPERIENCE_SOULBOUND);
    }
}
