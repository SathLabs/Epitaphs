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
    MESSAGE_GRAVE_CREATED            ("grave_created",             "Created a grave with your items at %s"),
    MESSAGE_HIGHLIGHT_INFO           ("highlight_info",            "Use %s to find your way back to your grave"),
    MESSAGE_GRAVE_NO_ACCESS          ("grave_no_access",           "This grave belongs to %s. You cannot access it"),
    MESSAGE_GRAVE_OP_BYPASS          ("grave_op_bypass",           "You have sufficient permission to bypass this grave's access"),
    MESSAGE_RESTORE_FAILED           ("restore_failed",            "Something went wrong trying to restore your items"),
    MESSAGE_DISTANCE_TO_GRAVE        ("distance_to_grave",         "Distance to grave: %s"),
    MESSAGE_COPY_TO_CLIPBOARD        ("copy_to_clipboard",         "Copy to clipboard"),
    MESSAGE_AUTOFILL_COMMAND         ("autofill_command",          "Autofill command"),
    MESSAGE_SOULS_HINT               ("souls_hint",                "A soul is still trapped within this grave... maybe a bottle could work"),
    MESSAGE_SOULS_NONE               ("souls_none",                "No souls linger in this grave anymore"),
    MESSAGE_SOUL_BOTTLE_OBTAIN       ("soul_bottle_obtain",        "Obtained by using a bottle on a grave with a lingering soul"),
    MESSAGE_SOUL_BOTTLE_USE          ("soul_bottle_use",           "Will teleport you to your last grave after drinking"),
    MESSAGE_SOULBOUND_HINT           ("soulbound_hint",            "Applies the soulbound enchantment when combined in an anvil"),
    MESSAGE_EXPERIENCE_SOULBOUND_HINT("experience_soulbound_hint", "Combine soulbound armor with an experience bottle to apply experience soulbound"),
    MESSAGE_FILE_PURGING_WARNING     ("file_purging_warning",      "This server has automatic file purging enabled."),
    MESSAGE_FILE_PURGED_INFO         ("file_purged_info",          "This save was created before %s and automatically deleted!"),
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
