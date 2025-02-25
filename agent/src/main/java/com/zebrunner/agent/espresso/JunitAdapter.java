package com.zebrunner.agent.espresso;

import com.zebrunner.agent.core.registrar.TestRunRegistrar;
import com.zebrunner.agent.core.registrar.descriptor.Status;
import com.zebrunner.agent.core.registrar.descriptor.TestFinishDescriptor;
import com.zebrunner.agent.core.registrar.descriptor.TestRunFinishDescriptor;
import com.zebrunner.agent.core.registrar.descriptor.TestRunStartDescriptor;
import com.zebrunner.agent.core.registrar.descriptor.TestStartDescriptor;
import com.zebrunner.agent.espresso.core.TestCorrelationData;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

import lombok.SneakyThrows;

public class JunitAdapter {

    private static final Set<String> testRunTestIds = new ConcurrentSkipListSet<>();
    private static final TestRunRegistrar registrar = TestRunRegistrar.getInstance();

    private static Description rootSuiteDescription;

    public void registerRunStart(Description description) {
        if (rootSuiteDescription == null) {
            rootSuiteDescription = description;

            OffsetDateTime startedAt = OffsetDateTime.now();
            String name = "Espresso test run [" + startedAt.toInstant() + " (UTC)]";
            TestRunStartDescriptor testRunStartDescriptor = new TestRunStartDescriptor(
                    name, "espresso", startedAt, null
            );

            registrar.registerStart(testRunStartDescriptor);
        }
    }

    public void registerRunFinish(Result result) {
        registrar.registerFinish(new TestRunFinishDescriptor(OffsetDateTime.now()));
    }

    @SneakyThrows
    public void registerTestStart(Description description) {
        String currentTestId = description.getDisplayName();
        TestCorrelationData testCorrelationData = this.buildTestCorrelationData(description);
        TestStartDescriptor testStartDescriptor = new TestStartDescriptor(
                testCorrelationData.asJsonString(),
                description.getMethodName(),
                OffsetDateTime.now(),
                description.getTestClass(),
                description.getTestClass().getDeclaredMethod(description.getMethodName()),
                null
        );

        testRunTestIds.add(currentTestId);
        registrar.registerTestStart(currentTestId, testStartDescriptor);
    }

    public void registerTestFinish(Description description) {
        String currentTestId = description.getDisplayName();

        if (testRunTestIds.contains(currentTestId)) {
            TestFinishDescriptor testFinishDescriptor = new TestFinishDescriptor(Status.PASSED);

            registrar.registerTestFinish(currentTestId, testFinishDescriptor);
            testRunTestIds.remove(currentTestId);
        }
    }

    public void registerTestFailure(Failure failure) {
        String currentTestId = failure.getDescription().getDisplayName();

        if (testRunTestIds.contains(currentTestId)) {
            TestFinishDescriptor result = new TestFinishDescriptor(Status.FAILED, OffsetDateTime.now(), failure.getTrace());

            registrar.registerTestFinish(currentTestId, result);
            testRunTestIds.remove(currentTestId);
        }
    }

    public void registerTestAssumptionFailure(Failure failure) {
        String currentTestId = failure.getDescription().getDisplayName();

        if (testRunTestIds.contains(currentTestId)) {
            TestFinishDescriptor result = new TestFinishDescriptor(Status.SKIPPED, OffsetDateTime.now(), failure.getTrace());

            registrar.registerTestFinish(currentTestId, result);
            testRunTestIds.remove(currentTestId);
        }
    }

    public void registerTestIgnored(Description description) {
        String currentTestId = description.getDisplayName();

        if (testRunTestIds.contains(currentTestId)) {
            TestFinishDescriptor result = new TestFinishDescriptor(Status.SKIPPED);

            registrar.registerTestFinish(currentTestId, result);
            testRunTestIds.remove(currentTestId);
        }
    }

    @SneakyThrows
    private TestCorrelationData buildTestCorrelationData(Description description) {
        return TestCorrelationData.builder()
                                  .className(description.getClassName())
                                  .methodName(description.getMethodName())
                                  .parameterClassNames(
                                          Arrays.stream(description.getTestClass().getMethod(description.getMethodName()).getParameterTypes())
                                                .map(Class::getName)
                                                .collect(Collectors.toList())
                                  )
                                  .displayName(description.getDisplayName())
                                  .build();
    }

}
