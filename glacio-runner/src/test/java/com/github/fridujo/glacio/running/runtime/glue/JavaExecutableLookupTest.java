package com.github.fridujo.glacio.running.runtime.glue;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import com.github.fridujo.glacio.model.DocString;
import com.github.fridujo.glacio.model.Step;
import com.github.fridujo.glacio.running.api.extension.ExtensionContext;
import com.github.fridujo.glacio.running.runtime.ExecutionResult;
import com.github.fridujo.glacio.running.runtime.Status;
import com.github.fridujo.glacio.sample.TestStepDef;

class JavaExecutableLookupTest {

    @Test
    void lookup_of_existing_given_methods() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Set<String> gluePaths = Collections.singleton("com.github.fridujo.glacio.sample");
        ExtensionContext extensionContext = mock(ExtensionContext.class);
        JavaExecutableLookup executableLookup = new JavaExecutableLookup(classLoader, gluePaths, emptySet(), extensionContext);
        Step step = stepWithText("a user named Aldoux");

        Executable lookup = executableLookup.lookup(step);

        assertThat(lookup).isNotNull();

        ExecutionResult executionResult = lookup.execute();

        assertThat(executionResult.getStatus()).as("Execution status " + executionResult).isEqualTo(Status.SUCCESS);
        TestStepDef glue = executableLookup.glueFactory.getGlue(TestStepDef.class);
        assertThat(glue.getName()).as("User name step argument").isEqualTo("Aldoux");
    }

    @Test
    void lookup_of_existing_when_methods() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Set<String> gluePaths = Collections.singleton("com.github.fridujo.glacio.sample");
        ExtensionContext extensionContext = mock(ExtensionContext.class);
        JavaExecutableLookup executableLookup = new JavaExecutableLookup(classLoader, gluePaths, emptySet(), extensionContext);
        Step step = stepWithText("the user clicks on the button");
        TestStepDef glue = executableLookup.glueFactory.getGlue(TestStepDef.class);

        Executable lookup = executableLookup.lookup(step);

        assertThat(glue.isClicked()).isFalse();
        ExecutionResult executionResult = lookup.execute();
        assertThat(executionResult.getStatus()).as("Execution status " + executionResult).isEqualTo(Status.SUCCESS);
        assertThat(glue.isClicked()).as("click action performed").isTrue();
    }

    @Test
    void execution_with_failed_assertion() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Set<String> gluePaths = Collections.singleton("com.github.fridujo.glacio.sample");
        ExtensionContext extensionContext = mock(ExtensionContext.class);
        ExecutableLookup executableLookup = new JavaExecutableLookup(classLoader, gluePaths, emptySet(), extensionContext);
        Step step = stepWithText("the button have been clicked on");

        Executable lookup = executableLookup.lookup(step);
        ExecutionResult executionResult = lookup.execute();

        assertThat(executionResult.getStatus()).as("Execution status " + executionResult).isEqualTo(Status.FAIL);
        assertThat(executionResult.getMessage()).isEqualTo(System.lineSeparator() +
            "Expecting:" + System.lineSeparator() +
            " <false>" + System.lineSeparator() +
            "to be equal to:" + System.lineSeparator() +
            " <true>" + System.lineSeparator() +
            "but was not.");
        assertThat(executionResult.getCause()).isExactlyInstanceOf(AssertionFailedError.class);
    }

    @Test
    void execution_with_failed_access() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Set<String> gluePaths = Collections.singleton("com.github.fridujo.glacio.sample");
        ExtensionContext extensionContext = mock(ExtensionContext.class);
        ExecutableLookup executableLookup = new JavaExecutableLookup(classLoader, gluePaths, emptySet(), extensionContext);
        Step step = stepWithText("package protected method");

        Executable lookup = executableLookup.lookup(step);
        ExecutionResult executionResult = lookup.execute();

        assertThat(executionResult.getStatus())
            .as("Execution status " + executionResult)
            .isEqualTo(Status.ABORT);
        assertThat(executionResult.getMessage())
            .contains(JavaExecutable.class.getName())
            .contains("not access a member of class " + TestStepDef.class.getName() + " with modifiers \"\"");
    }

    @Test
    void lookup_missing_method() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Set<String> gluePaths = Collections.singleton("com.github.fridujo.glacio.sample");
        ExtensionContext extensionContext = mock(ExtensionContext.class);
        ExecutableLookup executableLookup = new JavaExecutableLookup(classLoader, gluePaths, emptySet(), extensionContext);
        Step step = stepWithText("no match (test)");

        assertThatExceptionOfType(MissingStepImplementationException.class)
            .isThrownBy(() -> executableLookup.lookup(step))
            .withMessage("No matching step def found for step: no match (test)");
    }

    @Test
    void lookup_with_multiple_matching_method() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Set<String> gluePaths = Collections.singleton("com.github.fridujo.glacio.sample");
        ExtensionContext extensionContext = mock(ExtensionContext.class);
        ExecutableLookup executableLookup = new JavaExecutableLookup(classLoader, gluePaths, emptySet(), extensionContext);
        Step step = stepWithText("a user with data test");

        assertThatExceptionOfType(AmbiguousStepDefinitionsException.class)
            .isThrownBy(() -> executableLookup.lookup(step))
            .withMessageStartingWith("Step 'a user with data test' matches multiple Step Definitions: ")
            .withMessageContaining("TestStepDef#a_user_with")
            .withMessageContaining("TestStepDef#a_user_with_data");
    }

    @Test
    void execution_with_argument_parameter() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Set<String> gluePaths = Collections.singleton("com.github.fridujo.glacio.sample");
        ExtensionContext extensionContext = mock(ExtensionContext.class);
        JavaExecutableLookup executableLookup = new JavaExecutableLookup(classLoader, gluePaths, emptySet(), extensionContext);
        Step step = new Step(
            false,
            Optional.empty(),
            "a document",
            Optional.of(new DocString(Optional.of("md"), "# Test")),
            Collections.emptyList());

        TestStepDef glue = executableLookup.glueFactory.getGlue(TestStepDef.class);

        Executable lookup = executableLookup.lookup(step);

        assertThat(glue.getDocument()).isNull();
        ExecutionResult executionResult = lookup.execute();
        assertThat(executionResult.getStatus()).as("Execution status " + executionResult).isEqualTo(Status.SUCCESS);
        assertThat(glue.getDocument()).isEqualTo("# Test");
        assertThat(glue.getDocumentType()).isEqualTo("md");
    }

    private Step stepWithText(String text) {
        return new Step(false, Optional.empty(), text, Optional.empty(), Collections.emptyList());
    }
}
