//
// MIT License
//
// Copyright (c) 2024 Incendo
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//
package org.incendo.cloud.suggestion;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.apiguardian.api.API;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;

/**
 * Provider of suggestions
 *
 * @param <C> command sender type
 */
@API(status = API.Status.STABLE)
@FunctionalInterface
public interface SuggestionProvider<C> {

    /**
     * Returns a future that completes with the suggestions for the given {@code input}.
     *
     * <p>The {@code input} parameter contains all sender-provided input that has not yet been consumed by the argument parsers.
     * If the component that the suggestion provider is generating suggestions for consumes multiple tokens the suggestion
     * provider might receive a {@link CommandInput} instance containing multiple tokens.
     * {@link CommandInput#lastRemainingToken()} may be used to extract the part of the command that is currently being
     * completed by the command sender.</p>
     *
     * <p>If you don't need to return a future, you can implement {@link BlockingSuggestionProvider} instead.</p>
     *
     * @param context the context of the suggestion lookup
     * @param input   the current input
     * @return the suggestions
     */
    @NonNull CompletableFuture<@NonNull Iterable<@NonNull Suggestion>> suggestionsFuture(
            @NonNull CommandContext<C> context,
            @NonNull CommandInput input
    );

    /**
     * Get a suggestion provider that provides no suggestions.
     *
     * @param <C> command sender type
     * @return suggestion provider
     */
    static <C> SuggestionProvider<C> noSuggestions() {
        return (ctx, in) -> CompletableFuture.completedFuture(Collections.emptyList());
    }

    /**
     * Utility method to simplify implementing {@link BlockingSuggestionProvider}
     * using a lambda, for methods that accept a {@link SuggestionProvider}.
     *
     * @param blockingSuggestionProvider suggestion provider
     * @param <C>                        command sender type
     * @return suggestion provider
     */
    static <C> @NonNull SuggestionProvider<C> blocking(
            final @NonNull BlockingSuggestionProvider<C> blockingSuggestionProvider
    ) {
        return blockingSuggestionProvider;
    }

    /**
     * Utility method to simplify implementing {@link BlockingSuggestionProvider.Strings}
     * using a lambda, for methods that accept a {@link SuggestionProvider}.
     *
     * @param blockingStringsSuggestionProvider suggestion provider
     * @param <C>                               command sender type
     * @return suggestion provider
     */
    static <C> @NonNull SuggestionProvider<C> blockingStrings(
            final BlockingSuggestionProvider.@NonNull Strings<C> blockingStringsSuggestionProvider
    ) {
        return blockingStringsSuggestionProvider;
    }

    /**
     * Create a {@link SuggestionProvider} that provides constant suggestions.
     *
     * @param suggestions list of strings to suggest
     * @param <C>         command sender type
     * @return suggestion provider
     */
    static <C> @NonNull SuggestionProvider<C> suggesting(
            final @NonNull Suggestion @NonNull... suggestions
    ) {
        return suggesting(Arrays.asList(suggestions));
    }

    /**
     * Create a {@link SuggestionProvider} that provides constant string suggestions.
     *
     * @param suggestions list of strings to suggest
     * @param <C>         command sender type
     * @return suggestion provider
     */
    static <C> @NonNull SuggestionProvider<C> suggestingStrings(
            final @NonNull String @NonNull... suggestions
    ) {
        return suggestingStrings(Arrays.asList(suggestions));
    }

    /**
     * Create a {@link SuggestionProvider} that provides constant suggestions.
     *
     * @param suggestions list of strings to suggest
     * @param <C>         command sender type
     * @return suggestion provider
     */
    static <C> @NonNull SuggestionProvider<C> suggesting(
            final @NonNull Iterable<@NonNull Suggestion> suggestions
    ) {
        return blocking((ctx, input) -> suggestions);
    }

    /**
     * Create a {@link SuggestionProvider} that provides constant string suggestions.
     *
     * @param suggestions list of strings to suggest
     * @param <C>         command sender type
     * @return suggestion provider
     */
    static <C> @NonNull SuggestionProvider<C> suggestingStrings(
            final @NonNull Iterable<@NonNull String> suggestions
    ) {
        return blockingStrings((ctx, input) -> suggestions);
    }
}
