package dev.satherov.epitaphs.common.data;

import lombok.Getter;
import lombok.experimental.Accessors;

import dev.satherov.epitaphs.EPConfig;
import dev.satherov.sathlib.util.SLStringUtils;

import com.google.common.base.Supplier;

import java.nio.file.Path;
import java.time.Instant;
import java.util.regex.Pattern;

///
/// Defines the type of backup.
///
@Getter
@Accessors(fluent = true)
public enum BackupType {
    DEATH(
            () -> Pattern.compile(DataHandler.DATE_PATTERN.pattern() + "-death\\.dat(?:-old)?$"),
            () -> Pattern.compile(DataHandler.DATE_PATTERN.pattern() + "-death\\.dat-old$"),
            EPConfig.Server::getMaxOldDeaths
    ),
    SAVE(
            () -> Pattern.compile(DataHandler.DATE_PATTERN.pattern() + "-save\\.dat$"),
            EPConfig.Server::getMaxBackups
    ),
    ANY(
            () -> Pattern.compile(DataHandler.DATE_PATTERN.pattern() + "-(?:death|save)\\.dat(?:-old)?$"),
            () -> Integer.MIN_VALUE
    );
    
    private final Supplier<Pattern> pattern;
    private final Supplier<Pattern> purgePattern;
    private final Supplier<Integer> limit;
    
    BackupType(Supplier<Pattern> pattern, Supplier<Integer> limit) {
        this.pattern = pattern;
        this.purgePattern = pattern;
        this.limit = limit;
    }
    
    BackupType(Supplier<Pattern> pattern, Supplier<Pattern> purgePattern, Supplier<Integer> limit) {
        this.pattern = pattern;
        this.purgePattern = purgePattern;
        this.limit = limit;
    }
    
    ///
    /// Gets the pattern used to match backup files.
    ///
    public Pattern pattern() {
        return this.pattern.get();
    }
    
    ///
    /// Gets the pattern used to purge backup files
    ///
    public Pattern purgePattern() {
        return this.purgePattern.get();
    }
    
    ///
    /// Gets the maximum number of backups allowed for this type.
    ///
    public int limit() {
        return this.limit.get();
    }
    
    ///
    /// Resolves the path to a backup file from the given parent folder and timestamp.
    ///
    /// @param parent Parent folder path. Should be a player's uuid.
    /// @param now    Timestamp of the backup.
    ///
    public Path resolve(Path parent, Instant now) {
        String timestamp = DataHandler.FORMATTER.format(now);
        return parent.resolve(timestamp + "-" + SLStringUtils.lower(this.name()) + ".dat");
    }
}
