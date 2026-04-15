package dev.satherov.epitaphs;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.UtilityClass;

import dev.satherov.epitaphs.common.data.DataHandler;
import dev.satherov.epitaphs.config.data.Comment;
import dev.satherov.epitaphs.config.data.Config;
import dev.satherov.epitaphs.config.data.ConfigEntry;
import dev.satherov.epitaphs.config.data.ConfigEnum;
import dev.satherov.epitaphs.config.data.ConfigHolder;
import dev.satherov.epitaphs.config.data.Range;

import net.neoforged.fml.config.ModConfig;

import java.time.format.DateTimeFormatter;

@UtilityClass
@ConfigHolder
public final class EPConfig {
    
    @Config(ModConfig.Type.CLIENT)
    public static final class Client {
        
        @ConfigEntry
        @Comment("Decides which time-date formatter to use for the grave tooltips")
        private static @Getter @Setter Formatter tooltipFormatter = Formatter.SYSTEM;
        
        @Getter
        @RequiredArgsConstructor
        @Accessors(fluent = true)
        public enum Formatter implements ConfigEnum<Formatter> {
            ISO8601("Display Grave Tooltips in the ISO8601 format", DataHandler.ISO8601_FORMATTER),
            SYSTEM("Display Grave Tooltips adjusted to the System Settings", DataHandler.SYSTEM_FORMATTER),
            ;
            
            private final String description;
            private final DateTimeFormatter formatter;
        }
    }
    
    @Config(ModConfig.Type.SERVER)
    public static final class Server {
        
        @Range(min = 0)
        @ConfigEntry
        @Comment("Time in minutes between an automatic backup of all players. 0 to disable")
        private static @Getter @Setter int backupInterval = 10;
        
        @Range(min = 1)
        @ConfigEntry
        @Comment("Maximum number of backups to keep")
        @Comment("Backups are taken automatically every X minutes")
        private static @Getter @Setter int maxBackups = 10;
        
        @Range(min = 1)
        @ConfigEntry
        @Comment("Maximum number of old deaths to keep")
        @Comment("Old deaths are from graves which have already been claimed")
        private static @Getter @Setter int maxOldDeaths = 5;
    }
}
