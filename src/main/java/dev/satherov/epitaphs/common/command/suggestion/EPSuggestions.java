package dev.satherov.epitaphs.common.command.suggestion;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EPSuggestions extends Suggestions {

    private static final EPSuggestions EMPTY = new EPSuggestions(StringRange.at(0), new ArrayList<>());

    public EPSuggestions(StringRange range, List<Suggestion> suggestions) {
        super(range, suggestions);
    }

    public EPSuggestions reverse() {
        List<Suggestion> reversedSuggestions = new ArrayList<>(this.getList());
        Collections.reverse(reversedSuggestions);
        return new EPSuggestions(this.getRange(), reversedSuggestions);
    }

    public static EPSuggestions create(final String command, final Collection<Suggestion> suggestions) {
        if (suggestions.isEmpty()) {
            return new EPSuggestions(StringRange.at(0), new ArrayList<>());
        }

        int start = Integer.MAX_VALUE;
        int end = Integer.MIN_VALUE;
        for (final Suggestion suggestion : suggestions) {
            start = Math.min(suggestion.getRange().getStart(), start);
            end = Math.max(suggestion.getRange().getEnd(), end);
        }
        final StringRange range = new StringRange(start, end);
        final Set<Suggestion> texts = new HashSet<>();
        for (final Suggestion suggestion : suggestions) {
            texts.add(suggestion.expand(command, range));
        }
        final List<Suggestion> sorted = new ArrayList<>(texts);
        sorted.sort(Suggestion::compareToIgnoreCase);
        return new EPSuggestions(range, sorted);
    }

}
