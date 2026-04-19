package dev.satherov.epitaphs.data.provider.tags;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.core.EPRegistry;

import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ItemTagsProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;

import org.jetbrains.annotations.NotNull;

import top.theillusivec4.curios.api.CuriosTags;

import java.util.concurrent.CompletableFuture;

public class EPItemTagProvider extends ItemTagsProvider {
    
    public EPItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, Epitaphs.MOD_ID);
    }
    
    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        
        this.tag(EPRegistry.SOULBOUND_ENCHANTABLE)
                .addTag(Tags.Items.ENCHANTABLES)
                .addTag(Tags.Items.TOOLS)
                .addOptionalTag(CuriosTags.BACK)
                .addOptionalTag(CuriosTags.BELT)
                .addOptionalTag(CuriosTags.BODY)
                .addOptionalTag(CuriosTags.BRACELET)
                .addOptionalTag(CuriosTags.CHARM)
                .addOptionalTag(CuriosTags.CURIO)
                .addOptionalTag(CuriosTags.createItemTag("feet"))
                .addOptionalTag(CuriosTags.HANDS)
                .addOptionalTag(CuriosTags.HEAD)
                .addOptionalTag(CuriosTags.NECKLACE)
                .addOptionalTag(CuriosTags.RING);
        
        this.tag(EPRegistry.EXPERIENCE_SOULBOUND_ENCHANTABLE)
                .addTag(Tags.Items.ARMORS);
    }
}
