package dev.satherov.epitaphs.client.lang;

import dev.satherov.epitaphs.Epitaphs;

import net.minecraft.Util;

public enum EPLanguage implements ILangEntry {

    BLOCK_GRAVE("block", "grave"),

    COMMAND_RESTORE_SUCCESS("command", "restore_success"),
    COMMAND_RESTORE_FAILED("command", "restore_failed"),
    COMMAND_RESTORE_OVERFLOW("command", "restore_overflow"),
    COMMAND_BACKUP_SUCCESS("command", "backup_success"),
    COMMAND_BACKUP_FAILED("command", "backup_failed"),

    COMMAND_NOT_FOUND("command", "not_found"),

    CONTAINER_GRAVE("container", "grave"),

    ENCHANTMENT_SOULBOUND("enchantment", "soulbound"),
    ENCHANTMENT_EXPERIENCE_SOULBOUND("enchantment", "experience_soulbound"),

    MESSAGE_NO_ACCESS("message", "no_access"),
    MESSAGE_GRAVE_SUCCESS("message", "grave_success"),
    MESSAGE_GRAVE_NO_POS("message", "grave_no_pos"),
    MESSAGE_GRAVE_FAILED("message", "grave_failed"),
    MESSAGE_GRAVE_ERROR("message", "grave_error"),
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
