package dev.satherov.epitaphs;

import net.neoforged.neoforge.common.ModConfigSpec;

public class EpitaphsConfig {
    
    static class Server {
        
        private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
        
        private static final ModConfigSpec.IntValue BACKUP_INTERVAL = BUILDER
                .comment("Time in minutes between a backup of all currently online players")
                .defineInRange("backupInterval", 5, 1, Integer.MAX_VALUE);
        
        private static final ModConfigSpec.IntValue MAX_BACKUPS = BUILDER
                .comment("Maximum number of backups to keep.")
                .comment("Backups are save states taken periodically")
                .defineInRange("maxBackups", 10, 1, Integer.MAX_VALUE);
        
        private static final ModConfigSpec.IntValue MAX_OLD = BUILDER
                .comment("Maximum number of old saves to keep.")
                .comment("Old saves are grave save states which are no longer associated with a grave")
                .defineInRange("maxBackups", 10, 1, Integer.MAX_VALUE);
        
        public static final ModConfigSpec SPEC = BUILDER.build();
    }
    
    public static int getBackupInterval() {
        return Server.BACKUP_INTERVAL.get();
    }
    
    public static int getMaxBackups() {
        return Server.MAX_BACKUPS.get();
    }
    
    public static int getMaxOld() {
        return Server.MAX_OLD.get();
    }
}
