package dev.satherov.epitaphs.client.lang;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import dev.satherov.epitaphs.Epitaphs;

import net.minecraft.Util;

import java.util.function.BiConsumer;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum EPLanguage implements EPTranslatable {
    // @formatter:off
    CREATIVE_TAB_DEFAULT            ("itemGroup",       "default",                  "Epitaphs"),
    
    MESSAGE_GRAVE_CREATED           ("message",         "grave_created",            "Created a grave with your items at %s"),
    MESSAGE_HIGHLIGHT_INFO          ("message",         "highlight_info",           "Use %s to find your way back to your grave"),
    MESSAGE_GRAVE_NO_ACCESS         ("message",         "grave_no_access",          "This grave belongs to %s. You cannot access it"),
    MESSAGE_GRAVE_OP_BYPASS         ("message",         "grave_op_bypass",          "You have sufficient permission to bypass this grave's access"),
    MESSAGE_RESTORE_FAILED          ("message",         "restore_failed",           "Something went wrong trying to restore your items"),
    MESSAGE_DISTANCE_TO_GRAVE       ("message",         "distance_to_grave",        "Distance to grave: %s"),
    MESSAGE_COPY_TO_CLIPBOARD       ("message",         "copy_to_clipboard",        "Copy to clipboard"),
    MESSAGE_AUTOFILL_COMMAND        ("message",         "autofill_command",         "Autofill command"),
    
    ENCHANTMENT_SOULBOUND           ("enchantment",     "soulbound",                "Soulbound"),
    ENCHANTMENT_EXPERIENCE_SOULBOUND("enchantment",     "experience_soulbound",     "Experience Soulbound"),
    
    COMMAND_RESET_SUCCESS           ("command",         "reset.success",            "Successfully reset %s to %s"),
    COMMAND_RESET_FAILURE           ("command",         "reset.failure",            "Failed to reset %s to %s"),
    
    COMMAND_RECOVER_SUCCESS         ("command",         "recover.success",          "Successfully recovered %s to %s"),
    COMMAND_RECOVER_FAILURE         ("command",         "recover.failure",          "Failed to recover %s to %s"),
    
    COMMAND_FILES_EMPTY             ("command",         "files.empty",              "No backups available for %s "),
    COMMAND_FILES_SUCCESS           ("command",         "files.success",            "The following backups are available for %s:"),
    
    COMMAND_SAVE_SUCCESS_FULL       ("command",         "save.success.full",        "Successfully saved all players at %s"),
    COMMAND_SAVE_SUCCESS_SINGLE     ("command",         "save.success.single",      "Successfully saved player %s at %s"),
    COMMAND_SAVE_FAILURE_FULL       ("command",         "save.failure.full",        "Failed to save any player at %s"),
    COMMAND_SAVE_FAILURE_PARTIAL    ("command",         "save.failure.partial",     "Failed to save some players at %s"),
    COMMAND_SAVE_FAILURE_SINGLE     ("command",         "save.failure.single",      "Failed to save player %s at %s"),
    
    COMMAND_UUID_RESULT             ("command",         "uuid.result",              "Player %s has the following uuid %s"),
    
    COMMAND_LIST_SUCCESS            ("command",         "list.success",             "Player %s has the following grave locations:"),
    COMMAND_LIST_LATEST             ("command",         "list.latest",              "Your latest death was at %s in %s"),
    COMMAND_LIST_EMPTY              ("command",         "list.empty",               "No position data available for %s"),
    COMMAND_LIST_FAILURE_FILE       ("command",         "list.failure.file",        "Failed to read position data for %s from file"),
    
    COMMAND_HIGHLIGHT_CLEAR         ("command",         "highlight.clear",          "Cleared last tracked location"),
    COMMAND_HIGHLIGHT_INVALID       ("command",         "highlight.invalid",        "Invalid death timestamp '%s'"),
    COMMAND_HIGHLIGHT_SUCCESS       ("command",         "highlight.success",        "Set new highlight location to %s in %s"),
    // @formatter:on
    ;
    
    private final String key;
    private final String translation;
    
    EPLanguage(String type, String key, String translation) {
        this(Util.makeDescriptionId(type, Epitaphs.id(key)), translation);
    }
    
    public static void translate(BiConsumer<String, String> consumer) {
        for (EPLanguage lang : EPLanguage.values()) {
            consumer.accept(lang.key(), lang.translation());
        }
    }
}
