package dev.satherov.epitaphs.compat;

import net.neoforged.fml.ModList;

import java.util.function.Supplier;

public class CompatHandler {
    
    public static final String CURIOS = "curios";
    
    public static <T> T run(String mod, Supplier<T> method, T defaultValue) {
         if (ModList.get().isLoaded(mod)) return method.get();
         return defaultValue;
    }
    
    public static void run(String mod, Runnable method) {
         if (ModList.get().isLoaded(mod)) method.run();
    }
}
