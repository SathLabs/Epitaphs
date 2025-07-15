package dev.satherov.epitaphs.datagen.assets;


import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.client.lang.EPLanguage;

import net.neoforged.neoforge.common.data.LanguageProvider;

import net.minecraft.data.PackOutput;

public class EPLanguageProvider extends LanguageProvider {

    public EPLanguageProvider(PackOutput output) {
        super(output, Epitaphs.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add(EPLanguage.CONTAINER_GRAVE, "Grave");
        add(EPLanguage.BLOCK_GRAVE, "Grave");
        add(EPLanguage.MESSAGE_NO_ACCESS, "This grave belongs to %s");
        add(EPLanguage.MESSAGE_GRAVE_SUCCESS, "Created a grave with your items at");
        add(EPLanguage.MESSAGE_GRAVE_FAILED, "Found no valid position for a grave"); // You can recover your items via /epitaphs recover
        add(EPLanguage.MESSAGE_GRAVE_ERROR, "An error occurred trying to load this grave. Check your logs for more info");
    }

    private void add(EPLanguage lang, String translation) {
        add(lang.getTranslationKey(), translation);
    }
}
