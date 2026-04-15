package dev.satherov.epitaphs.config.data;

///
/// Marks an enum as a config enum.
///
public interface ConfigEnum<E extends Enum<E> & ConfigEnum<E>> {
    
    ///
    /// Human-readable description of what the specific enum value does.
    ///
    /// @return Description of the enum value.
    ///
    String description();
}
