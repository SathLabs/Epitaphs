package dev.satherov.epitaphs.client.lang;

import lombok.Getter;
import lombok.experimental.Accessors;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.sathlib.client.lang.SLTranslatable;

import net.minecraft.util.Util;

import java.util.function.BiConsumer;

@Getter
@Accessors(fluent = true)
public enum EPMessageLang implements SLTranslatable {
    // @formatter:off
    MESSAGE_GRAVE_CREATED           ("grave_created",            "Created a grave with your items at %s"),
    MESSAGE_HIGHLIGHT_INFO          ("highlight_info",           "Use %s to find your way back to your grave"),
    MESSAGE_GRAVE_NO_ACCESS         ("grave_no_access",          "This grave belongs to %s. You cannot access it"),
    MESSAGE_GRAVE_OP_BYPASS         ("grave_op_bypass",          "You have sufficient permission to bypass this grave's access"),
    MESSAGE_RESTORE_FAILED          ("restore_failed",           "Something went wrong trying to restore your items"),
    MESSAGE_DISTANCE_TO_GRAVE       ("distance_to_grave",        "Distance to grave: %s"),
    MESSAGE_COPY_TO_CLIPBOARD       ("copy_to_clipboard",        "Copy to clipboard"),
    MESSAGE_AUTOFILL_COMMAND        ("autofill_command",         "Autofill command"),
    // @formatter:on
    ;
    
    private final String key;
    private final String translation;
    
    EPMessageLang(String key, String translation) {
        this.key = Util.makeDescriptionId("message", Epitaphs.id(key));
        this.translation = translation;
    }
    
    public static void translate(BiConsumer<String, String> consumer) {
        for (SLTranslatable lang : EPMessageLang.values()) {
            consumer.accept(lang.key(), lang.translation());
        }
    }
}
