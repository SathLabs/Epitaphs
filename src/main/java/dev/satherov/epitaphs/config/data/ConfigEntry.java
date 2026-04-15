package dev.satherov.epitaphs.config.data;

import dev.satherov.epitaphs.util.StringUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

///
/// Marks a field as a config value. Must use a specific type to be valid
///
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigEntry {
    
    ///
    /// Config key of the value.
    /// By default, the field name will be used with {@link StringUtils#toSnakeCase(String)}
    ///
    /// @return Config key of the value.
    ///
    String value() default "";
    
    ///
    /// English translation for the config value.
    /// By default, the config key will be used with {@link StringUtils#toTitleCase(String)
    /// }
    ///
    /// @return English translation for the config value.
    ///
    String translation() default "";
}
