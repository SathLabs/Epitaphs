package dev.satherov.epitaphs.common.command.suggestion;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;

public interface EPSuggestionProvider<S> extends SuggestionProvider<S> {

    CompletableFuture<Suggestions> getSuggestions(final CommandContext<S> context, final EPSuggestionBuilder builder) throws CommandSyntaxException;

    default CompletableFuture<Suggestions> getSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) throws CommandSyntaxException {
        return this.getSuggestions(context, new EPSuggestionBuilder(builder.getInput(), builder.getStart()));
    }

}
