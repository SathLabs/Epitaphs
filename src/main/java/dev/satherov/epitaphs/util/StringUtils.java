package dev.satherov.epitaphs.util;

import lombok.experimental.UtilityClass;

import dev.satherov.epitaphs.core.annotations.NothingNull;

import org.intellij.lang.annotations.PrintFormat;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@NothingNull
@UtilityClass
public final class StringUtils {
    
    private static final Locale LOCALE = Locale.ROOT;
    
    ///
    /// Converts the given string to lowercase.
    ///
    /// @param string String to convert.
    ///
    /// @return Lowercased string with {@link Locale#ROOT} applied.
    ///
    public static String lower(String string) {
        return string.toLowerCase(StringUtils.LOCALE);
    }
    
    ///
    /// Converts the given object to a lower case string
    ///
    /// @param object Object to convert
    ///
    /// @return Lowercased string with {@link Locale#ROOT} applied.
    ///
    public static String lower(Object object) {
        return StringUtils.lower(String.valueOf(object));
    }
    
    ///
    /// Converts the given string to uppercase.
    ///
    /// @param string String to convert.
    ///
    /// @return Uppercased string with {@link Locale#ROOT} applied.
    ///
    public static String upper(String string) {
        return string.toUpperCase(StringUtils.LOCALE);
    }
    
    ///
    /// Converts the given object to uppercase.
    ///
    /// @param object Object to convert.
    ///
    /// @return Uppercased string with {@link Locale#ROOT} applied.
    ///
    public static String upper(Object object) {
        return StringUtils.upper(String.valueOf(object));
    }
    
    ///
    /// Formats the given string using the given arguments.
    ///
    /// @param string String to format.
    /// @param args   Arguments to format the string with.
    ///
    /// @return Formatted string with {@link Locale#ROOT} applied
    ///
    public static String format(@PrintFormat String string, Object... args) {
        return String.format(StringUtils.LOCALE, string, args);
    }
    
    ///
    /// Converts the given string to camel case.
    /// <p>
    /// {@code hello-world_thisIs  aTEST} -> {@code helloWorldThisIsATest}
    /// </p>
    ///
    /// @param input String to convert.
    ///
    /// @return Camel cased string.
    ///
    public static String toCamelCase(String input) {
        List<String> words = StringUtils.words(input);
        if (words.isEmpty()) return "";
        
        StringBuilder result = new StringBuilder();
        result.append(words.getFirst());
        
        for (int index = 1; index < words.size(); index++) {
            String word = words.get(index);
            result.append(StringUtils.capitalize(word));
        }
        
        return result.toString();
    }
    
    ///
    /// Converts the given string to pascal case.
    /// <p>
    /// {@code hello-world_thisIs  aTEST} -> {@code HelloWorldThisIsATest}
    /// </p>
    ///
    /// @param input String to convert.
    ///
    /// @return Pascal cased string.
    ///
    public static String toPascalCase(String input) {
        List<String> words = StringUtils.words(input);
        if (words.isEmpty()) return "";
        
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            result.append(StringUtils.capitalize(word));
        }
        
        return result.toString();
    }
    
    ///
    /// Converts the given string to snake case.
    /// <p>
    /// {@code hello-world_thisIs  aTEST} -> {@code hello_world_this_is_a_test}
    /// </p>
    ///
    /// @param input String to convert.
    ///
    /// @return Snake cased string.
    ///
    public static String toSnakeCase(String input) {
        List<String> words = StringUtils.words(input);
        return String.join("_", words);
    }
    
    ///
    /// Converts the given string to screaming snake case.
    /// <p>
    /// {@code hello-world_thisIs  aTEST} -> {@code HELLO_WORLD_THIS_IS_A_TEST}
    /// </p>
    ///
    /// @param input String to convert.
    ///
    /// @return Screaming snake case string.
    ///
    public static String toScreamingSnakeCase(String input) {
        List<String> words = StringUtils.words(input);
        
        String result = String.join("_", words);
        return result.toUpperCase(StringUtils.LOCALE);
    }
    
    ///
    /// Converts the given string to kebab case.
    /// <p>
    /// {@code hello-world_thisIs  aTEST} -> {@code hello-world-this-is-a-test}
    /// </p>
    ///
    /// @param input String to convert.
    ///
    /// @return Kebab cased string.
    ///
    public static String toKebabCase(String input) {
        List<String> words = StringUtils.words(input);
        return String.join("-", words);
    }
    
    ///
    /// Converts the given string to sentence case.
    /// <p>
    /// {@code hello-world_thisIs  aTEST} -> {@code Hello world this is a test}
    /// </p>
    ///
    /// @param input String to convert.
    ///
    /// @return Sentence cased string.
    ///
    public static String toSentenceCase(String input) {
        List<String> words = StringUtils.words(input);
        if (words.isEmpty()) return "";
        
        words.set(0, StringUtils.capitalize(words.getFirst()));
        return String.join(" ", words);
    }
    
    ///
    /// Converts the given string to title case.
    /// <p>
    /// {@code hello-world_thisIs  aTEST} -> {@code Hello World This Is A Test}
    /// </p>
    ///
    /// @param input String to convert.
    ///
    /// @return Title cased string.
    ///
    public static String toTitleCase(String input) {
        List<String> words = StringUtils.words(input);
        if (words.isEmpty()) return "";
        
        words.replaceAll(StringUtils::capitalize);
        return String.join(" ", words);
    }
    
    ///
    /// Capitalizes the first letter of the given word and turns the rest into lowercase.
    /// <p>
    /// {@code eXaMpLe} -> {@code Example}
    /// </p>
    ///
    /// @param word Word to capitalize.
    ///
    /// @return Capitalized word.
    ///
    public static String capitalize(@Nullable String word) {
        if (word == null || word.isEmpty()) return "";
        return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase(StringUtils.LOCALE);
    }
    
    ///
    /// Formats a floating point number with the given precision.
    ///
    /// @param value     Floating point number to format.
    /// @param precision Number of digits after the decimal point.
    ///
    /// @return Formatted decimal string.
    ///
    public static String decimal(double value, int precision) {
        final String base = String.valueOf(value);
        if (base.length() - 1 <= precision) return base;
        final String format = "%." + precision + "f";
        return String.format(Locale.ROOT, format, value);
    }
    
    ///
    /// Formats a floating point number with the given precision in scientific notation.
    ///
    /// @param value     Floating point number to format.
    /// @param precision Number of digits after the decimal point.
    ///
    /// @return Formatted decimal string in scientific notation.
    ///
    public static String scientific(double value, int precision) {
        final String base = String.valueOf(value);
        if (base.length() - 1 <= precision) return base;
        final String format = "%." + precision + "e";
        return String.format(Locale.ROOT, format, value);
    }
    
    ///
    /// Splits the given string into words.
    ///
    /// @param input String to split.
    ///
    /// @return List of words.
    ///
    private static List<String> words(@Nullable String input) {
        if (input == null || input.isBlank()) return new ArrayList<>(0);
        
        String[] parts = input
                .replaceAll("[_\\-]+", " ")
                .replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ")
                .replaceAll("[^A-Za-z0-9 ]+", " ")
                .trim()
                .split("\\s+");
        
        List<String> words = new ArrayList<>(parts.length);
        
        for (String part : parts) {
            if (part.isEmpty()) continue;
            words.add(part.toLowerCase(StringUtils.LOCALE));
        }
        
        return words;
    }
}
