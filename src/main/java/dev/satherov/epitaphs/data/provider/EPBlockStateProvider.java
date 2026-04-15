package dev.satherov.epitaphs.data.provider;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.core.EPRegistry;

import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import net.minecraft.data.PackOutput;

public class EPBlockStateProvider extends BlockStateProvider {
    
    public EPBlockStateProvider(PackOutput output, ExistingFileHelper fileHelper) {
        super(output, Epitaphs.MOD_ID, fileHelper);
    }
    
    @Override
    protected void registerStatesAndModels() {
        this.simpleBlock(EPRegistry.GRAVE.get(), this.models().getExistingFile(this.modLoc("block/grave")));
    }
}
