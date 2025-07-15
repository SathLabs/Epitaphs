package dev.satherov.epitaphs.common.data;

import dev.satherov.epitaphs.core.annotations.NothingNull;

import net.minecraft.util.StringRepresentable;

import java.nio.file.Path;
import java.util.List;

@NothingNull
public enum EPSaveType implements StringRepresentable {
    DEATH,
    SAVE,
    ANY;

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase();
    }

    public List<String> getExtensions() {
        return switch (this) {
            case DEATH -> List.of("-death.dat", "-death.dat-old");
            case SAVE -> List.of("-save.dat", "-save.dat-old");
            case ANY -> List.of("-death.dat", "-death.dat-old", "-save.dat", "-save.dat-old");
        };
    }
}
