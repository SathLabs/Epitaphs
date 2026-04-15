package dev.satherov.epitaphs.config.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

///
/// Adds a comment to a config value.
///
@Repeatable(Comments.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Comment {
    
    ///
    /// Comment to add.
    ///
    /// @return Comment
    ///
    String value();
}
