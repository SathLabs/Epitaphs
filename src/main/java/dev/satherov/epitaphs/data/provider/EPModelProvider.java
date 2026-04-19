package dev.satherov.epitaphs.data.provider;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.core.EPRegistry;

import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplate;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;

public class EPModelProvider extends ModelProvider {
    
    public EPModelProvider(PackOutput output) {
        super(output, Epitaphs.MOD_ID);
    }
    
    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        Block grave = EPRegistry.GRAVE.get();
        Identifier graveBlockModelLoc = this.modLocation("block/grave");
        Identifier graveItemModelLoc = this.modLocation("item/grave");
        
        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(
                        grave,
                        BlockModelGenerators.plainVariant(graveBlockModelLoc)
                )
        );
        
        ExtendedModelTemplate graveItemTemplate = ExtendedModelTemplateBuilder.builder()
                .parent(graveBlockModelLoc)
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, transform -> transform
                        .rotation(65.0F, 135.0F, 0.0F)
                        .translation(1.0F, 3.0F, 1.0F)
                        .scale(0.35F)
                )
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, transform -> transform
                        .rotation(0.0F, 0.0F, 0.0F)
                        .translation(0.0F, 2.5F, 1.0F)
                        .scale(0.40F)
                )
                .transform(ItemDisplayContext.GUI, transform -> transform
                        .rotation(30.0F, 45.0F, 0.0F)
                        .scale(0.5F)
                )
                .transform(ItemDisplayContext.GROUND, transform -> transform
                        .translation(0.0F, 3.0F, 0.0F)
                        .scale(0.35F)
                )
                .transform(ItemDisplayContext.FIXED, transform -> transform
                        .scale(0.5F)
                )
                .build();
        
        graveItemTemplate.create(
                graveItemModelLoc,
                new TextureMapping(),
                blockModels.modelOutput
        );
        
        itemModels.itemModelOutput.accept(
                grave.asItem(),
                ItemModelUtils.plainModel(graveItemModelLoc)
        );
    }
}