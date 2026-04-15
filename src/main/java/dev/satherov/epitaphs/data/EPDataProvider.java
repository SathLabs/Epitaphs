package dev.satherov.epitaphs.data;


import dev.satherov.epitaphs.Epitaphs;
import dev.satherov.epitaphs.core.annotations.NothingNull;

import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@NothingNull
public class EPDataProvider implements DataProvider {
    
    private final boolean server;
    private final DataGenerator generator;
    private final ExistingFileHelper fileHelper;
    private final PackOutput output;
    private final CompletableFuture<HolderLookup.Provider> lookup;
    private final RegistrySetBuilder builder;
    private final List<DataProvider> providers = new ArrayList<>();
    
    private EPDataProvider(GatherDataEvent event) {
        this.generator = event.getGenerator();
        this.fileHelper = event.getExistingFileHelper();
        this.output = this.generator.getPackOutput();
        this.lookup = event.getLookupProvider();
        this.builder = new RegistrySetBuilder();
        this.server = event.includeServer();
    }
    
    public static EPDataProvider create(GatherDataEvent event) {
        return new EPDataProvider(event);
    }
    
    private <T extends DataProvider> void add(T provider) {
        this.providers.add(provider);
    }
    
    public <T extends DataProvider> void add(DataProviderFromOutput<T> builder) {
        this.add(builder.create(this.output));
    }
    
    public <T extends DataProvider> void add(DataProviderFromOutputLookup<T> builder) {
        this.add(builder.create(this.output, this.lookup));
    }
    
    public <T extends DataProvider> void add(DataProviderFromOutputFileHelper<T> builder) {
        this.add(builder.create(this.output, this.fileHelper));
    }
    
    public <T extends DataProvider> void add(DataProviderFromOutputLookupFileHelper<T> builder) {
        this.add(builder.create(this.output, this.lookup, this.fileHelper));
    }
    
    public <T> void add(ResourceKey<? extends Registry<T>> key, RegistrySetBuilder.RegistryBootstrap<T> bootstrap) {
        this.builder.add(key, bootstrap);
    }
    
    public void generate() {
        this.generator.addProvider(true, this);
        this.generator.addProvider(this.server, (Factory<DatapackBuiltinEntriesProvider>) output -> new DatapackBuiltinEntriesProvider(output, this.lookup, this.builder, Set.of(Epitaphs.MOD_ID)));
    }
    
    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> list = new ArrayList<>();
        for (DataProvider provider : this.providers) list.add(provider.run(output));
        return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new));
    }
    
    @Override
    public String getName() {
        return "Epitaphs Data Provider";
    }
    
    @FunctionalInterface
    public interface DataProviderFromOutput<T extends DataProvider> {
        T create(PackOutput output);
    }
    
    @FunctionalInterface
    public interface DataProviderFromOutputLookup<T extends DataProvider> {
        T create(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider);
    }
    
    @FunctionalInterface
    public interface DataProviderFromOutputLookupFileHelper<T extends DataProvider> {
        T create(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper fileHelper);
    }
    
    @FunctionalInterface
    public interface DataProviderFromOutputFileHelper<T extends DataProvider> {
        T create(PackOutput output, ExistingFileHelper fileHelper);
    }
}
