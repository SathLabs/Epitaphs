package dev.satherov.epitaphs.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Map;

@Slf4j
@UtilityClass
public final class ReflectionUtils {
    
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = Map.of(
            boolean.class, Boolean.class,
            int.class, Integer.class,
            long.class, Long.class,
            double.class, Double.class
    );
    
    private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE = Map.of(
            Boolean.class, boolean.class,
            Integer.class, int.class,
            Long.class, long.class,
            Double.class, double.class
    );
    
    ///
    /// Normalizes primitive types to their wrapper counterparts.
    /// ```java
    /// int.class -> Integer.class
    /// boolean.class -> Boolean.class
    /// long.class -> Long.class
    /// double.class -> Double.class
    /// ```
    ///
    /// @param type Type to normalize.
    ///
    /// @return Normalized type or the original type if it is not a primitive.
    ///
    public static Class<?> getWrapper(Class<?> type) {
        Class<?> wrapper = ReflectionUtils.PRIMITIVE_TO_WRAPPER.get(type);
        return wrapper != null ? wrapper : type;
    }
    
    ///
    /// Normalizes wrapper types to their primitive counterparts.
    /// ```java
    /// Integer.class -> int.class
    /// Boolean.class -> boolean.class
    /// Long.class -> long.class
    /// Double.class -> double.class
    /// ```
    ///
    /// @param type Type to normalize.
    ///
    /// @return Normalized type or the original type if it is not a wrapper.
    ///
    public static Class<?> getPrimitive(Class<?> type) {
        Class<?> primitive = ReflectionUtils.WRAPPER_TO_PRIMITIVE.get(type);
        return primitive != null ? primitive : type;
    }
    
    ///
    /// Finds a {@link VarHandle} for the given field of the given type within the given owner class
    ///
    /// @param owner Owner class to search in
    /// @param type  Type of the field
    /// @param name  Name of the field
    ///
    /// @return VarHandle for the field
    ///
    /// @throws RuntimeException If the field cannot be found
    ///
    public static VarHandle findVarHandle(Class<?> owner, Class<?> type, String name) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(owner, MethodHandles.lookup());
            return lookup.findVarHandle(owner, name, type);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to create VarHandle for " + owner.getName() + "#" + name, e);
        }
    }
    
    ///
    /// Loads a class instance from the given module and class name
    ///
    /// @param module Module to load the class from.
    /// @param name   Class name to load.
    ///
    /// @return Class instance.
    ///
    /// @throws RuntimeException If the class cannot be loaded, and we are running in the IDE.
    ///
    public static @Nullable Class<?> loadClass(Module module, String name) {
        return Class.forName(module, name);
    }
}
