package dev.satherov.epitaphs.data.provider;


import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.client.lang.EPCommandLang;
import dev.satherov.epitaphs.client.lang.EPEnchantmentLang;
import dev.satherov.epitaphs.client.lang.EPMessageLang;
import dev.satherov.epitaphs.core.EPRegistry;
import dev.satherov.sathlib.config.SLConfigLoader;

import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.data.LanguageProvider;

import net.minecraft.data.PackOutput;

public class EPLanguageProvider extends LanguageProvider {
    
    public EPLanguageProvider(PackOutput output) {
        super(output, Epitaphs.MOD_ID, "en_us");
    }
    
    @Override
    protected void addTranslations() {
        this.add("creative_tab.epitaphs", "Epitaphs");
        this.addBlock(EPRegistry.GRAVE, "Grave");
        this.addItem(EPRegistry.SOUL_BOTTLE, "Soul in a Bottle");
        EPEnchantmentLang.translate(this::add);
        EPCommandLang.translate(this::add);
        EPMessageLang.translate(this::add);
        SLConfigLoader.translate(ModList.get().getModContainerById(Epitaphs.MOD_ID).orElseThrow(), this::add);
    }
}
