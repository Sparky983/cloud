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
package org.incendo.cloud.annotations;

import io.leangen.geantyref.TypeToken;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotation.specifier.Quoted;
import org.incendo.cloud.annotation.specifier.Range;
import org.incendo.cloud.annotations.parser.Parser;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserParameters;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static com.google.common.truth.Truth.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnnotationParserTest {

    private static final List<Suggestion> NAMED_SUGGESTIONS = Arrays.asList("Dancing-Queen", "Gimme!-Gimme!-Gimme!",
            "Waterloo"
    ).stream().map(Suggestion::simple).collect(Collectors.toList());

    private CommandManager<TestCommandSender> manager;
    private AnnotationParser<TestCommandSender> annotationParser;
    private Collection<org.incendo.cloud.Command<TestCommandSender>> commands;

    @BeforeAll
    void setup() {
        manager = new TestCommandManager();
        annotationParser = new AnnotationParser<>(manager, TestCommandSender.class);
        manager.parserRegistry().registerNamedParserSupplier("potato", p -> new StringParser<>(StringParser.StringMode.SINGLE));
        /* Register a suggestion provider */
        manager.parserRegistry().registerSuggestionProvider(
                "some-name",
                SuggestionProvider.suggesting(NAMED_SUGGESTIONS)
        );
        /* Register a parameter injector */
        manager.parameterInjectorRegistry().registerInjector(
                InjectableValue.class,
                (context, annotations) -> new InjectableValue("Hello World!")
        );
        /* Register a builder modifier */
        annotationParser.registerBuilderModifier(
                IntegerArgumentInjector.class,
                (injector, builder) -> builder.required(injector.value(), IntegerParser.integerParser())
        );
        /* Parse the class. Required for both testMethodConstruction() and testNamedSuggestionProvider() */
        commands = new ArrayList<>();
        commands.addAll(annotationParser.parse(this));
        commands.addAll(annotationParser.parse(new ClassCommandMethod()));
    }

    @Test
    void testMethodConstruction() {
        Assertions.assertFalse(commands.isEmpty());
        manager.commandExecutor().executeCommand(new TestCommandSender(), "test literal 10").join();
        manager.commandExecutor().executeCommand(new TestCommandSender(), "t literal 10 o").join();
        manager.commandExecutor().executeCommand(new TestCommandSender(), "proxycommand 10").join();
        Assertions.assertThrows(CompletionException.class, () ->
                manager.commandExecutor().executeCommand(new TestCommandSender(), "test 101").join());
        manager.commandExecutor().executeCommand(new TestCommandSender(), "flagcommand -p").join();
        manager.commandExecutor().executeCommand(new TestCommandSender(), "flagcommand --print --word peanut").join();
        manager.commandExecutor().executeCommand(new TestCommandSender(), "parserflagcommand -s \"Hello World\"").join();
        manager.commandExecutor().executeCommand(new TestCommandSender(), "parserflagcommand -s \"Hello World\" -o This is a test").join();
        manager.commandExecutor().executeCommand(new TestCommandSender(), "class method").join();
    }

    @Test
    void testNamedSuggestionProvider() {
        Assertions.assertEquals(
                NAMED_SUGGESTIONS,
                manager.suggestionFactory().suggestImmediately(new TestCommandSender(), "namedsuggestions ").list()
        );
    }

    @Test
    void testAnnotationResolver() throws Exception {
        final Class<AnnotatedClass> annotatedClass = AnnotatedClass.class;
        final Method annotatedMethod = annotatedClass.getDeclaredMethod("annotatedMethod");

        System.out.println("Looking for @CommandDescription");
        final CommandDescription commandDescription = AnnotationParser.getMethodOrClassAnnotation(
                annotatedMethod,
                CommandDescription.class
        );
        Assertions.assertNotNull(commandDescription);
        Assertions.assertEquals("Hello World!", commandDescription.value());

        System.out.println("Looking for @Permission");
        final Permission permission = AnnotationParser.getMethodOrClassAnnotation(
                annotatedMethod,
                Permission.class
        );
        Assertions.assertNotNull(permission);
        Assertions.assertEquals("some.permission", permission.value());

        System.out.println("Looking for @Command");
        final Command command = AnnotationParser.getMethodOrClassAnnotation(
                annotatedMethod,
                Command.class
        );
        Assertions.assertNotNull(command);
        Assertions.assertEquals("method", command.value());

        System.out.println("Looking for @Regex");
        @SuppressWarnings("unused") final Regex regex = AnnotationParser.getMethodOrClassAnnotation(annotatedMethod, Regex.class);
    }

    @Test
    void testParameterInjection() {
        manager.commandExecutor().executeCommand(new TestCommandSender(), "injected 10").join();
    }

    @Test
    void testAnnotatedSuggestionProviders() {
        final SuggestionProvider<TestCommandSender> suggestionProvider =
                this.manager.parserRegistry().getSuggestionProvider("cows").orElse(null);

        assertThat(suggestionProvider).isNotNull();
        assertThat(suggestionProvider.suggestionsFuture(new CommandContext<>(new TestCommandSender(), manager),
                CommandInput.empty()).join()).contains(Suggestion.simple("Stella"));
    }

    @Test
    void testAnnotatedArgumentParser() {
        final ArgumentParser<TestCommandSender, CustomType> parser = this.manager.parserRegistry().createParser(
                TypeToken.get(CustomType.class),
                ParserParameters.empty()
        ).orElseThrow(() -> new NullPointerException("Could not find CustomType parser"));
        final CommandContext<TestCommandSender> context = new CommandContext<>(
                new TestCommandSender(),
                this.manager
        );
        assertThat(parser.parse(context, CommandInput.empty()).parsedValue().orElse(new CustomType("")).toString())
                .isEqualTo("yay");
        assertThat(parser.suggestionProvider().suggestionsFuture(context, CommandInput.empty()).join())
                .contains(Suggestion.simple("Stella"));
    }

    @Test
    @SuppressWarnings("unchecked_cast")
    void testMultiAliasedCommands() {
        final Collection<org.incendo.cloud.Command<TestCommandSender>> commands = annotationParser.parse(new AliasedCommands());

        // Find the root command that we are looking for.
        for (final org.incendo.cloud.Command<TestCommandSender> command : commands) {
            if (command.rootComponent().aliases().contains("acommand")) {
                assertThat(command.rootComponent().aliases()).containsExactly("acommand", "analias", "anotheralias");

                return;
            }
        }

        throw new IllegalStateException("Couldn't find the root command 'acommand'");
    }

    @Test
    void testInjectedCommand() {
        manager.commandExecutor().executeCommand(new TestCommandSender(), "injected 10").join();
    }

    @Suggestions("cows")
    public List<String> cowSuggestions(final CommandContext<TestCommandSender> context, final String input) {
        return Arrays.asList("Stella", "Bella", "Agda");
    }

    @Parser(suggestions = "cows")
    public CustomType customTypeParser(final CommandContext<TestCommandSender> context, final CommandInput input) {
        return new CustomType("yay");
    }

    @IntegerArgumentInjector
    @Command("injected")
    public void injectedCommand(final CommandContext<TestCommandSender> context) {
        System.out.printf("Got an integer: %d\n", context.<Integer>get("number"));
    }

    @ProxiedBy("proxycommand")
    @Command("test|t literal <int> [string]")
    public void testCommand(
            final TestCommandSender sender,
            @Argument("int") @Range(max = "100") final int argument,
            @Argument(value = "string", parserName = "potato") @Default("potato") final String string
    ) {
        System.out.printf("Received int: %d and string '%s'\n", argument, string);
    }

    @Command("flagcommand")
    public void testFlags(
            final TestCommandSender sender,
            @Flag(value = "print", aliases = "p") final boolean print,
            @Flag(value = "word", aliases = "w") final String word
    ) {
        if (print) {
            System.out.println(word);
        }
    }

    @Command("parserflagcommand")
    public void testQuotedFlags(
            final TestCommandSender sender,
            @Flag(value = "sentence", aliases = "s") @Quoted final String sentence,
            @Flag(value = "other", aliases = "o") @Greedy final String otherStuff
    ) {
        System.out.println(sentence + (otherStuff == null ? "" : " " + otherStuff));
    }

    @Command("namedsuggestions <input>")
    public void testNamedSuggestionProviders(
            @Argument(value = "input", suggestions = "some-name") final String argument
    ) {
    }

    @Command("inject")
    public void testInjectedParameters(
            final InjectableValue injectableValue
    ) {
        System.out.printf("Injected value: %s\n", injectableValue.toString());
    }

    @Command("class")
    private static class ClassCommandMethod {

        @Command("method")
        public void annotatedMethod() {
            System.out.println("kekw");
        }
    }

    @Permission("some.permission")
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface AnnotatedAnnotation {

    }


    @Bad1
    @CommandDescription("Hello World!")
    private static class AnnotatedClass {

        @Command("method")
        @AnnotatedAnnotation
        public static void annotatedMethod() {
        }
    }


    @Bad2
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    private @interface Bad1 {

    }


    @Bad1
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    private @interface Bad2 {

    }


    private static final class InjectableValue {

        private final String value;

        private InjectableValue(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }


    private static final class CustomType {

        private final String value;

        private CustomType(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }


    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface IntegerArgumentInjector {

        /**
         * The name of the integer argument to insert
         *
         * @return Integer argument name
         */
        String value() default "number";
    }


    private static final class AliasedCommands {

        private static final String COMMAND_ALIASES = "acommand|analias|anotheralias";

        @Command("acommand")
        public void commandOne() {
        }

        @Command(COMMAND_ALIASES + " sub1")
        public void commandTwo() {
        }

        @Command(COMMAND_ALIASES + " sub2")
        public void commandThree() {
        }
    }
}
