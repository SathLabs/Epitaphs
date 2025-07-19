package dev.satherov.epitaphs.common.data;

import dev.satherov.epitaphs.core.annotations.NothingNull;

import net.minecraft.util.StringRepresentable;

@NothingNull
public enum EBackupType implements StringRepresentable {
    DEATH,
    SAVE;

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase();
    }
}
