package dev.satherov.epitaphs.datagen.assets;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.core.EPRegistry;

import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import net.minecraft.data.PackOutput;
import net.minecraft.world.item.ItemDisplayContext;

public class EPItemModelProvider extends ItemModelProvider {
    
    public EPItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Epitaphs.MOD_ID, existingFileHelper);
    }
    
    @Override
    protected void registerModels() {
        
        String path = EPRegistry.GRAVE.getId().getPath();
        getBuilder(path)
                .parent(getExistingFile(modLoc("block/" + path)))
                .transforms()
                .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                .rotation(65, 135, 0)
                .translation(1f, 3f, 1f)
                .scale(0.35f)
                .end()
                .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                .rotation(0, 0, 0)
                .translation(0f, 2.5f, 1.0f)
                .scale(0.40f)
                .end()
                .transform(ItemDisplayContext.GUI)
                .rotation(30, 45, 0)
                .scale(0.5f)
                .end()
                .transform(ItemDisplayContext.GROUND)
                .translation(0, 3, 0)
                .scale(0.35f)
                .end()
                .transform(ItemDisplayContext.FIXED)
                .scale(0.5f)
                .end()
                .end();
        
    }
}

