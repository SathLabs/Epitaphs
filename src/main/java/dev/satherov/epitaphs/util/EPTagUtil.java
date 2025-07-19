package dev.satherov.epitaphs.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import org.jetbrains.annotations.NotNull;

public class EPTagUtil {

    public static SafetyBuilder getSafe(CompoundTag root, String key) {
        return SafetyBuilder.getSafe(root, key);
    }

    public static class SafetyBuilder extends CompoundTag {

        private final CompoundTag tag;

        private SafetyBuilder(CompoundTag tag) {
            this.tag = tag;
        }

        private static SafetyBuilder of(CompoundTag tag) {
            return new SafetyBuilder(tag);
        }

        private static SafetyBuilder of() {
            return new SafetyBuilder(new CompoundTag());
        }

        public static SafetyBuilder getSafe(CompoundTag root, String key) {
            if (!root.contains(key)) return SafetyBuilder.of();
            return SafetyBuilder.of(root.getCompound(key));
        }

        public SafetyBuilder getSafe(String key) {
            if (!this.tag.contains(key)) return SafetyBuilder.of();
            return SafetyBuilder.of(this.tag.getCompound(key));
        }

        @NotNull
        public CompoundTag getFinal(String key) {
            if (!this.tag.contains(key)) return new CompoundTag();
            return this.tag.getCompound(key);
        }

        @NotNull
        public ListTag getFinal(String key, int tagType) {
            if (!this.tag.contains(key)) return new ListTag();
            return this.tag.getList(key, tagType);
        }
    }
}
