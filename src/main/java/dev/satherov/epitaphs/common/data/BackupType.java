package dev.satherov.epitaphs.common.data;

import lombok.Getter;
import lombok.experimental.Accessors;

import dev.satherov.epitaphs.EPConfig;
import dev.satherov.epitaphs.util.StringUtils;

import com.google.common.base.Supplier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
    /// Creates a path for the given timestamp.
    ///
    /// @param parent Parent folder path. Should be a player's uuid.
    /// @param now    Timestamp of the backup.
    ///
    public Path create(Path parent, Instant now) {
        String timestamp = DataHandler.FORMATTER.format(now);
        return parent.resolve(timestamp + "-" + StringUtils.lower(this.name()) + ".dat");
    }
    
    ///
    /// Resolves the path to an existing backup file in the given parent folder for the given timestamp.
    ///
    /// @param parent Parent folder path. Should be a player's uuid.
    /// @param now    Timestamp of the backup.
    ///
    public Path resolve(Path parent, Instant now) throws IOException {
        String timestamp = DataHandler.FORMATTER.format(now);
        Pattern pattern = this.pattern();
        String prefix = timestamp + "-";
        
        try (Stream<Path> paths = Files.list(parent)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.startsWith(prefix) && pattern.matcher(name).matches();
                    })
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "No backup of type " + this.name() + " found for timestamp " + timestamp + " in " + parent
                    ));
        } catch (IOException exception) {
            throw new IOException("Failed to resolve backup in " + parent, exception);
        }
    }
}
