package dev.satherov.epitaphs.data.provider.tags;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.core.EPRegistry;

import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.ftb.mods.ftbchunks.api.FTBChunksTags;

import java.util.concurrent.CompletableFuture;

public class EPBlockTagsProvider extends BlockTagsProvider {
    
    public EPBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, @Nullable ExistingFileHelper fileHelper) {
        super(output, lookup, Epitaphs.MOD_ID, fileHelper);
    }
    
    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        this.tag(FTBChunksTags.Blocks.INTERACT_WHITELIST_TAG)
                .add(EPRegistry.GRAVE.get());
        
        this.tag(Tags.Blocks.RELOCATION_NOT_SUPPORTED)
                .add(EPRegistry.GRAVE.get());
    }
}
