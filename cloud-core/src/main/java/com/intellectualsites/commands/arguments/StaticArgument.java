//
// MIT License
//
// Copyright (c) 2020 Alexander Söderberg
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
package com.intellectualsites.commands.arguments;

import com.intellectualsites.commands.arguments.parser.ArgumentParseResult;
import com.intellectualsites.commands.arguments.parser.ArgumentParser;
import com.intellectualsites.commands.context.CommandContext;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * {@link CommandArgument} type that recognizes fixed strings. This type does not parse variables.
 *
 * @param <C> Command sender type
 */
public final class StaticArgument<C> extends CommandArgument<C, String> {

    private StaticArgument(final boolean required, @Nonnull final String name, @Nonnull final String... aliases) {
        super(required, name, new StaticArgumentParser<>(name, aliases), String.class);
    }

    /**
     * Create a new static argument instance for a required command argument
     *
     * @param name    Argument name
     * @param aliases Argument aliases
     * @param <C>     Command sender type
     * @return Constructed argument
     */
    @Nonnull
    public static <C> StaticArgument<C> required(@Nonnull final String name,
                                                 @Nonnull final String... aliases) {
        return new StaticArgument<>(true, name, aliases);
    }

    /**
     * Create a new static argument instance for an optional command argument
     *
     * @param name    Argument name
     * @param aliases Argument aliases
     * @param <C>     Command sender type
     * @return Constructed argument
     */
    @Nonnull
    public static <C> StaticArgument<C> optional(@Nonnull final String name,
                                                 @Nonnull final String... aliases) {
        return new StaticArgument<>(false, name, aliases);
    }

    /**
     * Register a new alias
     *
     * @param alias New alias
     */
    public void registerAlias(@Nonnull final String alias) {
        ((StaticArgumentParser<C>) this.getParser()).acceptedStrings.add(alias);
    }

    /**
     * Get an immutable view of the aliases
     *
     * @return Immutable view of the argument aliases
     */
    @Nonnull
    public Set<String> getAliases() {
        return Collections.unmodifiableSet(((StaticArgumentParser<C>) this.getParser()).getAcceptedStrings());
    }

    /**
     * Get an immutable list of all aliases that are not the main literal
     *
     * @return Immutable view of the optional argument aliases
     */
    @Nonnull
    public List<String> getAlternativeAliases() {
        return Collections.unmodifiableList(new ArrayList<>(((StaticArgumentParser<C>) this.getParser()).acceptedStrings));
    }


    private static final class StaticArgumentParser<C> implements ArgumentParser<C, String> {

        private final String name;
        private final Set<String> acceptedStrings = new HashSet<>();
        private final Set<String> alternativeAliases = new HashSet<>();

        private StaticArgumentParser(@Nonnull final String name, @Nonnull final String... aliases) {
            this.name = name;
            this.acceptedStrings.add(this.name);
            this.acceptedStrings.addAll(Arrays.asList(aliases));
            this.alternativeAliases.addAll(Arrays.asList(aliases));
        }

        @Nonnull
        @Override
        public ArgumentParseResult<String> parse(@Nonnull final CommandContext<C> commandContext,
                                                 @Nonnull final Queue<String> inputQueue) {
            final String string = inputQueue.peek();
            if (string == null) {
                return ArgumentParseResult.failure(new NullPointerException("No input provided"));
            }
            for (final String acceptedString : this.acceptedStrings) {
                if (string.equalsIgnoreCase(acceptedString)) {
                    // Remove the head of the queue
                    inputQueue.remove();
                    return ArgumentParseResult.success(this.name);
                }
            }
            return ArgumentParseResult.failure(new IllegalArgumentException(string));
        }

        @Nonnull
        @Override
        public List<String> suggestions(@Nonnull final CommandContext<C> commandContext, @Nonnull final String input) {
            return Collections.singletonList(this.name);
        }

        /**
         * Get the accepted strings
         *
         * @return Accepted strings
         */
        @Nonnull
        public Set<String> getAcceptedStrings() {
            return this.acceptedStrings;
        }
    }

}