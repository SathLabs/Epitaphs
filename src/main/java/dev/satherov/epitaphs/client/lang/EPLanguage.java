package dev.satherov.epitaphs.client.lang;

import dev.satherov.epitaphs.Epitaphs;

import net.minecraft.Util;

public enum EPLanguage implements ILangEntry {
    CONTAINER_GRAVE("container", "grave")
    ;

    private final String key;

    EPLanguage(String type, String key) {
        this(Util.makeDescriptionId(type, Epitaphs.rl(key)));
    }

    EPLanguage(String key) {
        this.key = key;
    }

    @Override
    public String getTranslationKey() {
        return key;
    }
}
