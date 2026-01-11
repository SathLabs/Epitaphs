package dev.satherov.epitaphs.datagen.assets;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.core.EPRegistry;

import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import net.minecraft.data.PackOutput;

public class EPBlockStateProvider extends BlockStateProvider {
    
    public EPBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, Epitaphs.MOD_ID, exFileHelper);
    }
    
    @Override
    protected void registerStatesAndModels() {
        simpleBlock(EPRegistry.GRAVE.get(), models().getExistingFile(modLoc("block/grave")));
    }
}
