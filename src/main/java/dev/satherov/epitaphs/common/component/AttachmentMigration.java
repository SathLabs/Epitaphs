package dev.satherov.epitaphs.common.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

final class AttachmentMigration {
    
    static final UUID EMPTY_UUID = UUID.nameUUIDFromBytes(new byte[0]);
    static final Codec<UUID> UUID_CODEC = Codec.STRING.comapFlatMap(AttachmentMigration::readUuid, UUID::toString);
    static final Codec<Instant> TIMESTAMP_CODEC = Codec.STRING.comapFlatMap(AttachmentMigration::readTimestamp, Instant::toString);
    
    private static final DateTimeFormatter LEGACY_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
    
    private AttachmentMigration() {
    }
    
    static DataResult<UUID> readUuid(String value) {
        if (value == null || value.isBlank()) {
            return DataResult.success(AttachmentMigration.EMPTY_UUID);
        }
        
        try {
            return DataResult.success(UUID.fromString(value));
        } catch (IllegalArgumentException ignored) {
            return DataResult.error(() -> "Invalid UUID: " + value);
        }
    }
    
    static DataResult<Instant> readTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return DataResult.success(Instant.EPOCH);
        }
        
        try {
            return DataResult.success(Instant.parse(value));
        } catch (DateTimeParseException ignored) {
        }
        
        try {
            return DataResult.success(LocalDateTime.parse(value, AttachmentMigration.LEGACY_TIMESTAMP).toInstant(ZoneOffset.UTC));
        } catch (DateTimeParseException ignored) {
            return DataResult.error(() -> "Invalid timestamp: " + value);
        }
    }
}
