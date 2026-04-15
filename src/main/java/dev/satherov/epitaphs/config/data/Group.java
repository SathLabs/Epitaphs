package dev.satherov.epitaphs.config.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

///
/// Defines a class as a config group. Should be within a config class annotated with {@link Config}.
///
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Group {
    
    ///
    /// Name of the config group. Defaults to the class name.
    ///
    /// @return Name of the config group.
    ///
    String value() default "";
}
