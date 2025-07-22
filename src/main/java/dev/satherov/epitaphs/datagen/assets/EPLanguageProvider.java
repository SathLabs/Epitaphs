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

        add(EPLanguage.BLOCK_GRAVE, "Grave");

        add(EPLanguage.COMMAND_RESTORE_SUCCESS, "Restored items of player %s");
        add(EPLanguage.COMMAND_RESTORE_FAILED, "Failed to restore items of player %s. Check your logs for more info");

        add(EPLanguage.COMMAND_BACKUP_SUCCESS, "Backup complete");
        add(EPLanguage.COMMAND_BACKUP_FAILED, "Failed to backup. Check your logs for more info");

        add(EPLanguage.COMMAND_NOT_FOUND, "No grave found for for %s");

        add(EPLanguage.CONTAINER_GRAVE, "Grave");

        add(EPLanguage.ENCHANTMENT_SOULBOUND, "Soulbound");
        add(EPLanguage.ENCHANTMENT_EXPERIENCE_SOULBOUND, "Experience Soulbound");

        add(EPLanguage.MESSAGE_NO_ACCESS, "This grave belongs to %s. You do not have access to it");
        add(EPLanguage.MESSAGE_GRAVE_SUCCESS, "Created a grave with your items at");
        add(EPLanguage.MESSAGE_GRAVE_FAILED, "Found no valid position for a grave");
        add(EPLanguage.MESSAGE_GRAVE_ERROR, "An error occurred trying to load this grave. Check your logs for more info");


    }

    private void add(EPLanguage lang, String translation) {
        add(lang.getTranslationKey(), translation);
    }
}
