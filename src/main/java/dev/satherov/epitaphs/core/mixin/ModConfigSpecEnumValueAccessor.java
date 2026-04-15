package dev.satherov.epitaphs.core.mixin;

import net.neoforged.neoforge.common.ModConfigSpec;

import com.electronwill.nightconfig.core.EnumGetMethod;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.function.Supplier;

@Mixin(ModConfigSpec.EnumValue.class)
public interface ModConfigSpecEnumValueAccessor {
    
    @Invoker("<init>")
    static <E extends Enum<E>> ModConfigSpec.EnumValue<E> create(ModConfigSpec.Builder parent, List<String> path, Supplier<E> defaultSupplier, EnumGetMethod converter, Class<E> clazz) {
        throw new AssertionError();
    }
}
