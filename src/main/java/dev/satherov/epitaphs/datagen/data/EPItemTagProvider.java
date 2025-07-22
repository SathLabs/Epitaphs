package dev.satherov.epitaphs.datagen.data;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.core.EPRegistry;

import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.level.block.Block;

import org.jetbrains.annotations.Nullable;

import top.theillusivec4.curios.api.CuriosTags;

import java.util.concurrent.CompletableFuture;

public class EPItemTagProvider extends ItemTagsProvider {

    public EPItemTagProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            CompletableFuture<TagsProvider.TagLookup<Block>> blockTags,
            @Nullable ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, blockTags, Epitaphs.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {

        tag(EPRegistry.SOULBOUND_ENCHANTABLE)
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

        tag(EPRegistry.EXPERIENCE_SOULBOUND_ENCHANTABLE)
                .addTag(Tags.Items.ARMORS);
    }
}
