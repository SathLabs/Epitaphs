package dev.satherov.epitaphs.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MathUtils {
    
    ///
    /// Split an integer losslessly into a fixed number of parts
    ///
    /// @param value the integer to split
    /// @param parts the number of times to split the integer
    ///
    /// @return an array of integers with the split values
    ///
    public static int[] split(int value, int parts) {
        int[] result = new int[parts];
        int base = value / parts;
        int remainder = value % parts;
        for (int idx = 0; idx < parts; idx++) {
            result[idx] = base;
            if (idx < remainder) result[idx] += 1;
        }
        return result;
    }
}
