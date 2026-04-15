package dev.satherov.epitaphs.data.provider;


import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.client.lang.EPLanguage;
import dev.satherov.epitaphs.core.EPRegistry;

import net.neoforged.neoforge.common.data.LanguageProvider;

import net.minecraft.data.PackOutput;

public class EPLanguageProvider extends LanguageProvider {
    
    public EPLanguageProvider(PackOutput output) {
        super(output, Epitaphs.MOD_ID, "en_us");
    }
    
    @Override
    protected void addTranslations() {
        this.addBlock(EPRegistry.GRAVE, "Grave");
        EPLanguage.translate(this::add);
    }
}
