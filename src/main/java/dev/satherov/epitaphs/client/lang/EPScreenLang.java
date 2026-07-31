package dev.satherov.epitaphs.client.lang;

import lombok.Getter;
import lombok.experimental.Accessors;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.sathlib.client.lang.SLTranslatable;

import net.minecraft.util.Util;

import java.util.function.BiConsumer;

@Getter
@Accessors(fluent = true)
public enum EPScreenLang implements SLTranslatable {
    // @formatter:off
    PREVIEW_CURIOS               ("preview.curios",              "Curios"),
    PREVIEW_CURIOS_TOGGLE        ("preview.curios.toggle",       "Show curios"),
    PREVIEW_CURIOS_NONE          ("preview.curios.none",         "This backup has no curio slots"),
    PREVIEW_CURIOS_SLOT          ("preview.curios.slot",         "Slot: %s"),
    PREVIEW_CURIOS_COSMETIC      ("preview.curios.cosmetic",     "%s (Cosmetic)"),
    PREVIEW_COSMETICS_TOGGLE     ("preview.cosmetics.toggle",    "Toggle cosmetic curio slots"),
    PREVIEW_PAGE                 ("preview.page",                "Page %s / %s"),

    PREVIEW_GIVE_HINT            ("preview.give_hint",           "Click to give yourself this item"),

    PREVIEW_RESTORE              ("preview.restore",             "Restore"),
    PREVIEW_RESTORE_HINT         ("preview.restore.hint",        "Close the preview and put the recover command in the chat"),
    PREVIEW_BACKUPS              ("preview.backups",             "Backups"),
    PREVIEW_BACKUPS_HINT         ("preview.backups.hint",        "List every backup of this player"),
    PREVIEW_BACKUPS_TITLE        ("preview.backups.title",       "Backups (%s)"),
    PREVIEW_BACKUPS_BACK         ("preview.backups.back",        "Back"),
    PREVIEW_BACKUPS_BACK_HINT    ("preview.backups.back.hint",   "Return to the backup details"),
    PREVIEW_BACKUPS_EMPTY        ("preview.backups.empty",       "No backups found"),

    PREVIEW_SECTION_BACKUP       ("preview.section.backup",      "Backup"),
    PREVIEW_SECTION_LOCATION     ("preview.section.location",    "Location"),
    PREVIEW_SECTION_STATUS       ("preview.section.status",      "Status"),

    PREVIEW_ENTRY_TAKEN          ("preview.entry.taken",         "Taken"),
    PREVIEW_ENTRY_TYPE           ("preview.entry.type",          "Type"),
    PREVIEW_ENTRY_DIMENSION      ("preview.entry.dimension",     "Dimension"),
    PREVIEW_ENTRY_POSITION       ("preview.entry.position",      "Position"),
    PREVIEW_ENTRY_GAME_MODE      ("preview.entry.game_mode",     "Game mode"),
    PREVIEW_ENTRY_SCORE          ("preview.entry.score",         "Score"),

    PREVIEW_VALUE_UNKNOWN        ("preview.value.unknown",       "Unknown"),
    PREVIEW_VALUE_DEATH          ("preview.value.death",         "Death"),
    PREVIEW_VALUE_SAVE           ("preview.value.save",          "Save"),
    PREVIEW_VALUE_POSITION       ("preview.value.position",      "%s %s %s"),
    // @formatter:on
    ;
    
    private final String key;
    private final String translation;
    
    EPScreenLang(String key, String translation) {
        this.key = Util.makeDescriptionId("screen", Epitaphs.id(key));
        this.translation = translation;
    }
    
    public static void translate(BiConsumer<String, String> consumer) {
        for (SLTranslatable lang : EPScreenLang.values()) {
            consumer.accept(lang.key(), lang.translation());
        }
    }
}
