package dev.satherov.epitaphs.data.provider.tags;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.core.EPRegistry;

import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class EPBlockTagsProvider extends BlockTagsProvider {
    
    public EPBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, Epitaphs.MOD_ID);
    }
    
    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        //        this.tag(FTBChunksTags.Blocks.INTERACT_WHITELIST_TAG)
        //                .add(EPRegistry.GRAVE.get());
        
        this.tag(Tags.Blocks.RELOCATION_NOT_SUPPORTED)
                .add(EPRegistry.GRAVE.get());
    }
}
