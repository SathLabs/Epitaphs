package dev.satherov.epitaphs.client.lang;

import lombok.Getter;
import lombok.experimental.Accessors;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.sathlib.client.lang.SLTranslatable;

import net.minecraft.util.Util;

import java.util.function.BiConsumer;

@Getter
@Accessors(fluent = true)
public enum EPCommandLang implements SLTranslatable {
    // @formatter:off
    COMMAND_RESET_SUCCESS           ("reset.success",            "Successfully reset %s to %s"),
    COMMAND_RESET_FAILURE           ("reset.failure",            "Failed to reset %s to %s"),
    
    COMMAND_RECOVER_SUCCESS         ("recover.success",          "Successfully recovered %s to %s"),
    COMMAND_RECOVER_FAILURE         ("recover.failure",          "Failed to recover %s to %s"),
    
    COMMAND_FILES_EMPTY             ("files.empty",              "No backups available for %s "),
    COMMAND_FILES_SUCCESS           ("files.success",            "The following backups are available for %s:"),
    
    COMMAND_SAVE_SUCCESS_FULL       ("save.success.full",        "Successfully saved all players at %s"),
    COMMAND_SAVE_SUCCESS_SINGLE     ("save.success.single",      "Successfully saved player %s at %s"),
    COMMAND_SAVE_FAILURE_FULL       ("save.failure.full",        "Failed to save any player at %s"),
    COMMAND_SAVE_FAILURE_PARTIAL    ("save.failure.partial",     "Failed to save some players at %s"),
    COMMAND_SAVE_FAILURE_SINGLE     ("save.failure.single",      "Failed to save player %s at %s"),
    
    COMMAND_UUID_RESULT             ("uuid.result",              "Player %s has the following uuid %s"),
    
    COMMAND_LIST_SUCCESS            ("list.success",             "Player %s has the following grave locations:"),
    COMMAND_LIST_LATEST             ("list.latest",              "Your latest death was at %s in %s"),
    COMMAND_LIST_EMPTY              ("list.empty",               "No position data available for %s"),
    COMMAND_LIST_FAILURE_FILE       ("list.failure.file",        "Failed to read position data for %s from file"),
    
    COMMAND_HIGHLIGHT_CLEAR         ("highlight.clear",          "Cleared last tracked location"),
    COMMAND_HIGHLIGHT_INVALID       ("highlight.invalid",        "Invalid death timestamp '%s'"),
    COMMAND_HIGHLIGHT_SUCCESS       ("highlight.success",        "Set new highlight location to %s in %s"),
    
    // @formatter:on
    ;
    
    private final String key;
    private final String translation;
    
    EPCommandLang(String key, String translation) {
        this.key = Util.makeDescriptionId("command", Epitaphs.id(key));
        this.translation = translation;
    }
    
    public static void translate(BiConsumer<String, String> consumer) {
        for (SLTranslatable lang : EPCommandLang.values()) {
            consumer.accept(lang.key(), lang.translation());
        }
    }
}
