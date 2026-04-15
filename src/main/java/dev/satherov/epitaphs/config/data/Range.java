package dev.satherov.epitaphs.config.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

///
/// Clamps a numeric config value to a specific range.
///
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Range {
    
    double min() default Double.MIN_VALUE;
    
    double max() default Double.MAX_VALUE;
}
