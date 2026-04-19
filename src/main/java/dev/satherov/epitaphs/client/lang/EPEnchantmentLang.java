package dev.satherov.epitaphs.client.lang;

import lombok.Getter;
import lombok.experimental.Accessors;

import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.sathlib.client.lang.SLTranslatable;

import net.minecraft.util.Util;

import java.util.function.BiConsumer;

@Getter
@Accessors(fluent = true)
public enum EPEnchantmentLang implements SLTranslatable {
    // @formatter:off
    ENCHANTMENT_SOULBOUND                ("soulbound",                 "Soulbound"),
    ENCHANTMENT_SOULBOUND_DESC           ("soulbound.desc",            "Keeps this item on the player after death"),
                                                                                       
    ENCHANTMENT_EXPERIENCE_SOULBOUND     ("experience_soulbound",      "Experience Soulbound"),
    ENCHANTMENT_EXPERIENCE_SOULBOUND_DESC("experience_soulbound.desc", "Keeps this item on the player after death. Additionally keeps 1/4th of the players xp"),
    // @formatter:on
    ;
    
    private final String key;
    private final String translation;
    
    EPEnchantmentLang(String key, String translation) {
        this.key = Util.makeDescriptionId("enchantment", Epitaphs.id(key));
        this.translation = translation;
    }
    
    public static void translate(BiConsumer<String, String> consumer) {
        for (EPEnchantmentLang lang : EPEnchantmentLang.values()) {
            consumer.accept(lang.key(), lang.translation());
        }
    }
}
