package dev.satherov.epitaphs.client.lang;

import dev.satherov.epitaphs.Epitaphs;

import net.minecraft.Util;

public enum EPLanguage implements ILangEntry {
    CONTAINER_GRAVE("container", "grave"),

    BLOCK_GRAVE("block", "grave"),

    MESSAGE_NO_ACCESS("message", "no_access"),
    MESSAGE_GRAVE_SUCCESS("message", "grave_success"),
    MESSAGE_GRAVE_FAILED("message", "grave_failed"),
    MESSAGE_GRAVE_ERROR("message", "grave_error")
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
