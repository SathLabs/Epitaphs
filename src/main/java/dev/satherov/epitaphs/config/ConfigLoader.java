package dev.satherov.epitaphs.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import dev.satherov.epitaphs.config.data.Comment;
import dev.satherov.epitaphs.config.data.Config;
import dev.satherov.epitaphs.config.data.ConfigEntry;
import dev.satherov.epitaphs.config.data.ConfigHolder;
import dev.satherov.epitaphs.config.data.Group;
import dev.satherov.epitaphs.config.data.Range;
import dev.satherov.epitaphs.util.ReflectionUtils;
import dev.satherov.epitaphs.util.StringUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.javafmlmod.FMLModContainer;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforgespi.language.ModFileScanData;

import net.minecraft.SharedConstants;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public final class ConfigLoader {
    
    private static final Type CONFIG_HOLDER = Type.getType(ConfigHolder.class);
    
    ///
    /// {@link VarHandle} for {@link FMLModContainer#scanResults}
    ///
    private static final VarHandle SCAN_RESULTS = ReflectionUtils.findVarHandle(FMLModContainer.class, ModFileScanData.class, "scanResults");
    
    ///
    /// {@link VarHandle} for {@link FMLModContainer#layer}
    ///
    private static final VarHandle LAYER = ReflectionUtils.findVarHandle(FMLModContainer.class, Module.class, "layer");
    
    private static final Set<String> MODS = ConcurrentHashMap.newKeySet();
    private static final Map<String, Map<ModConfigSpec, Cache>> CACHE = new ConcurrentHashMap<>();
    
    ///
    /// Generates the config file for the given mod container. Should be called within the mod constructor.
    ///
    /// @param container Mod container to generate the config for.
    ///
    public static void discover(FMLModContainer container) {
        ConfigLoader.hook(container);
        
        ModFileScanData scanResults = ConfigLoader.getScanResults(container);
        Module layer = ConfigLoader.getLayer(container);
        if (scanResults == null || layer == null) {
            ConfigLoader.throwOrLog("Failed to get mod scan results or module layer for " + container.getModId());
            return;
        }
        
        for (ModFileScanData.AnnotationData annotation : scanResults.getAnnotations()) {
            if (!annotation.annotationType().equals(ConfigLoader.CONFIG_HOLDER)) continue;
            
            String name = annotation.clazz().getClassName();
            Class<?> clazz = ReflectionUtils.loadClass(layer, name);
            if (clazz == null) {
                ConfigLoader.throwOrLog("Failed to load config holder class " + name);
                continue;
            }
            
            ConfigLoader.generate(container, clazz);
        }
    }
    
    ///
    /// Hooks the config up to the mod event bus.
    ///
    /// @param container Mod container to hook the config for.
    ///
    private static void hook(FMLModContainer container) {
        String mod = container.getModId();
        if (!ConfigLoader.MODS.add(mod)) return; // Already hooked, nothing to do
        
        IEventBus bus = container.getEventBus();
        if (bus == null) {
            ConfigLoader.throwOrLog("No event bus available for " + mod);
            return;
        }
        
        Consumer<ModConfigEvent> executor = event -> {
            ModConfig config = event.getConfig();
            // Not our mod, nothing for us to concern with
            if (!config.getModId().equalsIgnoreCase(mod)) return;
            
            // Not a mod config spec, probably some other mod firing this event
            if (!(config.getSpec() instanceof ModConfigSpec spec)) return;
            
            Map<ModConfigSpec, Cache> caches = ConfigLoader.CACHE.get(config.getModId());
            if (caches == null) {
                ConfigLoader.log.debug("No cache available for {}", mod);
                return;
            }
            
            Cache cache = caches.get(spec);
            if (cache == null) {
                ConfigLoader.throwOrLog("No config cache found for '" + config.getFileName() + "'");
                return;
            }
            
            // Update the values inside our config files via reflection
            cache.getValues().forEach((field, value) -> {
                try {
                    field.setAccessible(true);
                    field.set(null, value.get());
                } catch (Throwable e) {
                    ConfigLoader.log.error("Failed to update config value for {}", field, e);
                }
            });
        };
        
        // Hook up the executor to the event bus
        bus.addListener(ModConfigEvent.Loading.class, executor::accept);
        bus.addListener(ModConfigEvent.Reloading.class, executor::accept);
    }
    
    ///
    /// Generates the config files contained within the {@link ConfigHolder} class.
    ///
    /// @param container Mod container to generate the config for.
    /// @param holder    Config holder class to generate the configs from.
    ///
    private static void generate(FMLModContainer container, Class<?> holder) {
        final String mod = container.getModId();
        final Set<ModConfig.Type> types = new HashSet<>();
        
        // Check every declared class inside our config holder class
        for (Class<?> clazz : holder.getDeclaredClasses()) {
            
            // Not annotated with {@link Config}, probably a config enum or some other helper
            if (!clazz.isAnnotationPresent(Config.class)) {
                ConfigLoader.log.debug("Class {} in class {} is not annotated with @Config", clazz.getName(), holder.getName());
                continue;
            }
            
            Config config = clazz.getAnnotation(Config.class);
            ModConfig.Type type = config.value();
            String name = ConfigLoader.getConfigName(clazz, mod, config, types);
            
            // Build the ModSpec and cache
            Cache cache = new Cache();
            ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
            ConfigLoader.build(mod, clazz, builder, cache);
            ModConfigSpec spec = builder.build();
            if (spec.isEmpty()) ConfigLoader.throwOrLog("ModConfigSpec '" + name + "' is empty!");
            
            cache.setSpec(spec);
            ConfigLoader.CACHE.computeIfAbsent(mod, $ -> new ConcurrentHashMap<>()).put(spec, cache);
            
            // Registers the config in a folder with the name of the mod-id and file name of the name + type
            container.registerConfig(type, spec, mod + "/" + name + ".toml");
        }
    }
    
    ///
    /// Builds a single config file
    ///
    /// @param mod     The ModId of the mod this config is being constructed for.
    /// @param clazz   Class to build the config from.
    /// @param builder Builder to build the config with.
    /// @param cache   Cache to store the config in.
    ///
    private static void build(String mod, Class<?> clazz, ModConfigSpec.Builder builder, Cache cache) {
        ConfigLoader.log.debug("Building config from class '{}'", clazz.getCanonicalName());
        
        // Recursively check inner classes
        for (Class<?> inner : clazz.getDeclaredClasses()) {
            // Not a group class, probably a config enum or some other helper.
            if (!inner.isAnnotationPresent(Group.class)) {
                ConfigLoader.log.debug("Class {} in class {} is not annotated with @Group", inner.getName(), clazz.getName());
                continue;
            }
            
            Group group = inner.getAnnotation(Group.class);
            String name = ConfigLoader.getGroupName(inner, group);
            
            // Add the group to the builder
            builder.push(name);
            ConfigLoader.build(mod, inner, builder, cache);
            builder.pop();
        }
        
        for (Field field : clazz.getDeclaredFields()) {
            final Annotation[] annotations = field.getAnnotations();
            final Optional<ConfigEntry> optional = Arrays.stream(annotations).map($ -> field.getAnnotation(ConfigEntry.class)).filter(Objects::nonNull).findFirst();
            if (optional.isEmpty()) {
                ConfigLoader.log.warn("Field {} in class {} is not annotated with @ConfigEntry", field.getName(), clazz.getName());
                continue;
            }
            
            final ConfigEntry entry = optional.get();
            final String key = entry.value();
            final String name = key.isBlank() ? StringUtils.toSnakeCase(field.getName()) : key;
            final List<String> comments = Arrays.stream(field.getAnnotationsByType(Comment.class))
                    .map(Comment::value)
                    .map(String::strip)
                    .filter(comment -> !comment.isBlank())
                    .toList();
            
            try {
                field.setAccessible(true);
                Object object = field.get(null);
                ModConfigSpec.ConfigValue<?> spec = ConfigHandlers.handle(mod, builder, name, field, object, comments).orElseThrow(() -> new IllegalArgumentException("Unsupported config field type " + field.getType()));
                cache.getValues().put(field, spec);
            } catch (IllegalArgumentException e) {
                ConfigLoader.throwOrLog("Failed to build config field " + name, e);
            } catch (IllegalAccessException e) {
                ConfigLoader.throwOrLog("Failed to access field " + name, e);
            } catch (NullPointerException e) {
                ConfigLoader.throwOrLog("Config field " + name + " is null or not static", e);
            }
        }
    }
    
    ///
    /// Loads the {@link ModFileScanData} from the given {@link FMLModContainer} using reflection.
    ///
    /// @param container Mod container to load the scan results from.
    ///
    /// @return Scan results.
    ///
    /// @throws RuntimeException If the scan results cannot be loaded, and we are running in the IDE.
    ///
    private static @Nullable ModFileScanData getScanResults(FMLModContainer container) {
        try {
            return (ModFileScanData) ConfigLoader.SCAN_RESULTS.get(container);
        } catch (Throwable e) {
            ConfigLoader.throwOrLog("Failed to get scan results for " + container.getModId(), e);
            return null;
        }
    }
    
    ///
    /// Loads the {@link Module} from the given {@link FMLModContainer} using reflection.
    ///
    /// @param container Mod container to load the module from.
    ///
    /// @return Module.
    ///
    /// @throws RuntimeException If the module cannot be loaded, and we are running in the IDE.
    ///
    private static @Nullable Module getLayer(FMLModContainer container) {
        try {
            return (Module) ConfigLoader.LAYER.get(container);
        } catch (Throwable e) {
            ConfigLoader.throwOrLog("Failed to get module for " + container.getModId(), e);
            return null;
        }
    }
    
    ///
    /// Throws if we are inside the IDE and otherwise logs an error.
    ///
    /// @param msg Message to log.
    /// @param e   Exception to log.
    ///
    private static void throwOrLog(String msg, Throwable e) {
        if (SharedConstants.IS_RUNNING_IN_IDE) throw new RuntimeException(msg, e);
        else ConfigLoader.log.error(msg, e);
    }
    
    ///
    /// Throws if we are inside the IDE and otherwise logs an error.
    ///
    /// @param msg Message to log.
    ///
    private static void throwOrLog(String msg) {
        if (SharedConstants.IS_RUNNING_IN_IDE) throw new RuntimeException(msg);
        else ConfigLoader.log.warn(msg);
    }
    
    ///
    /// Gets the name of the config file using the following pattern.
    /// <p> 1. Use the name specified in {@link Config#name()}.
    /// <p> 2. ModId plus the {@link ModConfig.Type} of the config.
    /// <p> 3. The {@link Class#getSimpleName()} of the config class.
    ///
    /// @param clazz  The config class.
    /// @param mod    The mod id.
    /// @param config The config annotation.
    /// @param types  The existing types of configs.
    ///
    /// @return The name of the config file.
    ///
    private static String getConfigName(Class<?> clazz, String mod, Config config, Set<ModConfig.Type> types) {
        final ModConfig.Type type = config.value();
        final String name = config.name();
        if (!name.isBlank()) return name;
        if (types.add(type)) return mod + "-" + type.extension();
        return StringUtils.toSnakeCase(clazz.getSimpleName());
    }
    
    ///
    /// Gets the group name using the following pattern.
    /// <p> 1. Use the name specified in {@link Group#value()}.
    /// <p> 2. The {@link Class#getSimpleName()} of the group class.
    ///
    /// @param clazz The group class.
    /// @param group The group annotation.
    ///
    /// @return The name of the group.
    ///
    private static String getGroupName(Class<?> clazz, Group group) {
        String name = group.value();
        if (!name.isBlank()) return name;
        return StringUtils.toSnakeCase(clazz.getSimpleName());
    }
    
    ///
    /// Cache of a mod spec and its values plus their associated fields.
    ///
    @NoArgsConstructor
    private static class Cache {
        @Getter @Setter
        public ModConfigSpec spec;
        @Getter
        public final Map<Field, ModConfigSpec.ConfigValue<?>> values = new IdentityHashMap<>();
    }
    
    public static void translate(ModContainer container, BiConsumer<String, String> consumer) {
        Collection<Cache> caches = ConfigLoader.CACHE.getOrDefault(container.getModId(), new HashMap<>()).values();
        if (caches.isEmpty()) return;
        caches.stream().map(Cache::getValues).map(Map::entrySet).flatMap(Collection::stream).forEach(entry -> {
            final Field field = entry.getKey();
            final ModConfigSpec.ConfigValue<?> value = entry.getValue();
            final Annotation[] annotations = field.getAnnotations();
            ConfigLoader.addEntryTranslation(field, value, annotations, consumer);
        });
    }
    
    private static void addEntryTranslation(Field field, ModConfigSpec.ConfigValue<?> value, Annotation[] annotations, BiConsumer<String, String> consumer) {
        final Optional<ConfigEntry> optional = Arrays.stream(annotations).map($ -> field.getAnnotation(ConfigEntry.class)).filter(Objects::nonNull).findFirst();
        if (optional.isEmpty()) throw new IllegalStateException("Cache fields must always have a ConfigEntry annotation");
        
        final ConfigEntry config = optional.get();
        final String key = value.getSpec().getTranslationKey();
        if (key == null) throw new IllegalStateException("Config fields must always have a translation key");
        
        final String path = String.join(".", value.getPath());
        final String translation = config.translation();
        final String name = translation.isBlank() ? StringUtils.toSentenceCase(path) : translation;
        consumer.accept(key, name);
        ConfigLoader.addTooltipTranslation(key, field, annotations, consumer);
    }
    
    private static void addTooltipTranslation(String key, Field field, Annotation[] annotations, BiConsumer<String, String> consumer) {
        final Set<Comment> comments = Arrays.stream(annotations).filter(a -> a.annotationType() == Comment.class).map($ -> field.getAnnotation(Comment.class)).collect(Collectors.toSet());
        if (comments.isEmpty()) return;
        
        try {
            StringBuilder builder = new StringBuilder();
            comments.forEach(comment -> builder.append(comment.value().strip()).append("\n"));
            builder.append("\n");
            ConfigLoader.addRangeTranslation(field, annotations, builder);
            builder.append("\n");
            builder.append("Default: ").append(field.get(null));
            consumer.accept(key + ".tooltip", builder.toString().strip());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access config field " + field.getName(), e);
        }
    }
    
    private static void addRangeTranslation(Field field, Annotation[] annotations, StringBuilder builder) {
        final Optional<Range> optional = Arrays.stream(annotations).map($ -> field.getAnnotation(Range.class)).filter(Objects::nonNull).findFirst();
        if (optional.isEmpty()) return;
        final Range range = optional.get();
        if (range.max() < Double.MAX_VALUE && range.min() > Double.MIN_VALUE) {
            builder.append("Range: ").append(StringUtils.scientific(range.min(), 6)).append(" ~ ").append(StringUtils.scientific(range.max(), 6));
        } else if (range.max() >= Double.MAX_VALUE) {
            builder.append("Range: >").append(StringUtils.scientific(range.min(), 6));
        } else if (range.min() <= Double.MIN_VALUE) {
            builder.append("Range: <").append(StringUtils.scientific(range.max(), 6));
        }
    }
}
