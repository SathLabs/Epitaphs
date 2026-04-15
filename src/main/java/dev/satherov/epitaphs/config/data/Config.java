package dev.satherov.epitaphs.config.data;

import net.neoforged.fml.config.ModConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

///
/// Defines a class as a config. Should be within a top level class annotated with {@link ConfigHolder}.
///
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Config {
    
    ///
    /// Type of the config.
    ///
    /// @return Type of the config.
    ///
    ModConfig.Type value();
    
    ///
    /// Name of the config. Defaults to the type. If multiple configs of the same type are present, defaults to the class name.
    ///
    /// @return Name of the config.
    ///
    String name() default "";
}
