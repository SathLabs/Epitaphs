package dev.satherov.epitaphs.config;

import lombok.experimental.UtilityClass;

import dev.satherov.epitaphs.config.data.ConfigEnum;
import dev.satherov.epitaphs.config.data.Range;
import dev.satherov.epitaphs.core.mixin.ModConfigSpecEnumValueAccessor;
import dev.satherov.epitaphs.util.ReflectionUtils;

import net.neoforged.neoforge.common.ModConfigSpec;

import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;

import com.electronwill.nightconfig.core.EnumGetMethod;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@UtilityClass
public final class ConfigHandlers {
    
    public static Optional<ModConfigSpec.ConfigValue<?>> handle(String mod, ModConfigSpec.Builder builder, String name, Field field, Object object, List<String> comments) {
        Class<?> type = ReflectionUtils.getWrapper(field.getType());
        ConfigHandler handler = ConfigHandlers.HANDLERS.get(type);
        if (handler == null && type.isEnum()) handler = ConfigHandlers.HANDLERS.get(Enum.class);
        if (handler == null) return Optional.empty();
        return Optional.of(handler.handle(builder.translation(Util.makeDescriptionId("config", ResourceLocation.fromNamespaceAndPath(mod, name))), name, field, object, comments));
    }
    
    private static final Map<Class<?>, ConfigHandler> HANDLERS = Map.of(
            String.class, new StringHandler(),
            Boolean.class, new BooleanHandler(),
            Integer.class, new IntegerHandler(),
            Long.class, new LongHandler(),
            Double.class, new DoubleHandler(),
            Enum.class, new EnumHandler(),
            List.class, new ListHandler()
    );
    
    @FunctionalInterface
    public interface ConfigHandler {
        ModConfigSpec.ConfigValue<?> handle(ModConfigSpec.Builder builder, String name, Field field, Object object, List<String> comments);
    }
    
    private static class StringHandler implements ConfigHandler {
        
        @Override
        public ModConfigSpec.ConfigValue<?> handle(ModConfigSpec.Builder builder, String name, Field field, Object object, List<String> comments) {
            String value = ConfigHandlers.validate(name, object, String.class);
            ConfigHandlers.comment(builder, comments, value);
            return builder.define(name, value);
        }
    }
    
    private static class BooleanHandler implements ConfigHandler {
        
        @Override
        public ModConfigSpec.ConfigValue<?> handle(ModConfigSpec.Builder builder, String name, Field field, Object object, List<String> comments) {
            boolean value = ConfigHandlers.validate(name, object, Boolean.class);
            ConfigHandlers.comment(builder, comments, value);
            return builder.define(name, value);
        }
    }
    
    private static class IntegerHandler implements ConfigHandler {
        
        @Override
        public ModConfigSpec.ConfigValue<?> handle(ModConfigSpec.Builder builder, String name, Field field, Object object, List<String> comments) {
            final MinMax range = MinMax.of(field);
            int value = ConfigHandlers.validate(name, object, Integer.class);
            ConfigHandlers.comment(builder, comments, value);
            return builder.defineInRange(name, value, (int) range.min(), (int) range.max());
        }
    }
    
    private static class LongHandler implements ConfigHandler {
        
        @Override
        public ModConfigSpec.ConfigValue<?> handle(ModConfigSpec.Builder builder, String name, Field field, Object object, List<String> comments) {
            final MinMax range = MinMax.of(field);
            long value = ConfigHandlers.validate(name, object, Long.class);
            ConfigHandlers.comment(builder, comments, value);
            return builder.defineInRange(name, value, (long) range.min(), (long) range.max());
        }
    }
    
    private static class DoubleHandler implements ConfigHandler {
        
        @Override
        public ModConfigSpec.ConfigValue<?> handle(ModConfigSpec.Builder builder, String name, Field field, Object object, List<String> comments) {
            final MinMax range = MinMax.of(field);
            double value = ConfigHandlers.validate(name, object, Double.class);
            ConfigHandlers.comment(builder, comments, value);
            return builder.defineInRange(name, value, range.min(), range.max());
        }
    }
    
    @SuppressWarnings("rawtypes")
    private static class EnumHandler implements ConfigHandler {
        
        @Override
        public ModConfigSpec.ConfigValue<?> handle(ModConfigSpec.Builder builder, String name, Field field, Object object, List<String> comments) {
            Class<? extends Enum> clazz = field.getType().asSubclass(Enum.class);
            Enum<?> value = ConfigHandlers.validate(name, object, clazz);
            if (!(value instanceof ConfigEnum<?>)) {
                throw new IllegalArgumentException("Enum " + clazz.getSimpleName() + " is not a Config Enum");
            }
            
            ConfigHandlers.comment(builder, comments, value);
            Arrays.stream(clazz.getEnumConstants()).forEach(e -> builder.comment(" " + e.name() + ": " + ((ConfigEnum<?>) e).description()));
            return EnumHandler.defineEnum(name, builder, value, clazz);
        }
        
        @SuppressWarnings("unchecked")
        private static <V extends Enum<V>> ModConfigSpec.EnumValue<V> defineEnum(String name, ModConfigSpec.Builder builder, Enum<?> value, Class<?> clazz) {
            return EnumHandler.createEnumValue(name, builder, (V) value, (Class<V>) clazz);
        }
        
        private static <V extends Enum<V>> ModConfigSpec.EnumValue<V> createEnumValue(String name, ModConfigSpec.Builder builder, V value, Class<V> clazz) {
            Collection<V> allowed = Arrays.asList(clazz.getEnumConstants());
            
            ModConfigSpec.ConfigValue<V> def = builder.define(Collections.singletonList(name), () -> value, obj -> {
                if (clazz.isInstance(obj)) return allowed.contains(clazz.cast(obj));
                try {
                    return allowed.contains(EnumGetMethod.NAME_IGNORECASE.get(obj, clazz));
                } catch (IllegalArgumentException | ClassCastException e) {
                    return false;
                }
            }, clazz);
            
            return ModConfigSpecEnumValueAccessor.create(
                    builder,
                    def.getPath(),
                    () -> value,
                    EnumGetMethod.NAME_IGNORECASE,
                    clazz
            );
        }
    }
    
    private static class ListHandler implements ConfigHandler {
        
        @Override
        public ModConfigSpec.ConfigValue<?> handle(ModConfigSpec.Builder builder, String name, Field field, Object object, List<String> comments) {
            List<?> value = ConfigHandlers.validate(name, object, List.class);
            ConfigHandlers.comment(builder, comments, value);
            return builder.defineList(name, value, null, $ -> true);
        }
    }
    
    private static <T> T validate(String name, Object object, Class<T> clazz) throws IllegalArgumentException, NullPointerException {
        if (object == null) {
            throw new NullPointerException("Config value " + name + " is null");
        }
        
        if (!clazz.isInstance(object)) {
            throw new IllegalArgumentException("Config value " + name + " is not of type " + clazz.getName());
        }
        
        return clazz.cast(object);
    }
    
    private static void comment(ModConfigSpec.Builder builder, List<String> comments, Object defaultValue) {
        for (String comment : comments) builder.comment(" " + comment);
        builder.comment(" Default: " + defaultValue);
    }
    
    private record MinMax(double min, double max) {
        private static final MinMax DEFAULT = new MinMax(Double.MIN_VALUE, Double.MAX_VALUE);
        
        private static MinMax of(Field field) {
            Range annotation = field.getAnnotation(Range.class);
            if (annotation == null) return MinMax.DEFAULT;
            return new MinMax(annotation.min(), annotation.max());
        }
    }
}
