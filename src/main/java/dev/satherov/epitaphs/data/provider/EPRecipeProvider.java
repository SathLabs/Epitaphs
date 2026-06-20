package dev.satherov.epitaphs.data.provider;

import dev.satherov.epitaphs.common.block.GraveBlock;
import dev.satherov.epitaphs.core.EPRegistry;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.Blocks;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class EPRecipeProvider extends RecipeProvider {
    
    
    protected EPRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }
    
    @Override
    protected void buildRecipes() {
        
        ItemStackTemplate template = new ItemStackTemplate(EPRegistry.GRAVE.get().asItem(), DataComponentPatch.builder()
                .set(DataComponents.BLOCK_STATE, GraveBlock.DECO_PROPERTIES)
                .build()
        );
        
        ShapedRecipeBuilder.shaped(this.items, RecipeCategory.MISC, template)
                .define('#', Blocks.STONE)
                .define('S', Blocks.STONE_SLAB)
                .define('X', Blocks.SMOOTH_STONE_SLAB)
                .pattern(" # ")
                .pattern("#X#")
                .pattern("SSS")
                .unlockedBy("has_stone", this.has(Blocks.STONE))
                .save(this.output);
    }
    
    public static final class Runner extends RecipeProvider.Runner {
        
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }
        
        @Override
        protected @NonNull EPRecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new EPRecipeProvider(registries, output);
        }
        
        @Override
        public @NonNull String getName() {
            return "Epitaphs Recipes Runner";
        }
    }
}
