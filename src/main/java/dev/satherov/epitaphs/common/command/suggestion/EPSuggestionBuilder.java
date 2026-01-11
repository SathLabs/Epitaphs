package dev.satherov.epitaphs.common.command.suggestion;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class EPSuggestionBuilder extends SuggestionsBuilder {
    
    private final String input;
    private final List<Suggestion> result = new ArrayList<>();
    
    public EPSuggestionBuilder(final String input, final String inputLowerCase, final int start) {
        super(input, inputLowerCase, start);
        this.input = input;
    }
    
    public EPSuggestionBuilder(final String input, final int start) {
        this(input, input.toLowerCase(Locale.ROOT), start);
    }
    
    @Override
    public EPSuggestions build() {
        return EPSuggestions.create(input, result);
    }
    
    public CompletableFuture<Suggestions> buildFutureReversed() {
        return CompletableFuture.completedFuture(build().reverse());
    }
}
