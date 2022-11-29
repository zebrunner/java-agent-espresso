package com.zebrunner.agent;

import com.google.gson.Gson;
import com.zebrunner.agent.core.registrar.TestRunRegistrar;
import com.zebrunner.agent.core.registrar.descriptor.Status;
import com.zebrunner.agent.core.registrar.descriptor.TestFinishDescriptor;
import com.zebrunner.agent.core.registrar.descriptor.TestRunFinishDescriptor;
import com.zebrunner.agent.core.registrar.descriptor.TestRunStartDescriptor;
import com.zebrunner.agent.core.registrar.descriptor.TestStartDescriptor;
import com.zebrunner.agent.core.TestInvocationContext;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import lombok.SneakyThrows;

public class AndroidJunitAdapter {

    private static final TestRunRegistrar registrar = TestRunRegistrar.getInstance();
    private static final Set<String> testRunTestIds = new ConcurrentSkipListSet<>();

    public void registerRunStart(Description description) {
        String name = description.getDisplayName();
        TestRunStartDescriptor testRunStartDescriptor = new TestRunStartDescriptor(
                name, "espresso", OffsetDateTime.now(), name
        );

        registrar.registerStart(testRunStartDescriptor);
    }

    public void registerRunFinish(Result result) {
        registrar.registerFinish(new TestRunFinishDescriptor(OffsetDateTime.now()));
    }

    @SneakyThrows
    public void registerTestStart(Description description) {
        Class<?> testClass = description.getTestClass();
        String correlationData = this.buildCorrelationData(description);
        Method method = testClass.getDeclaredMethod(description.getMethodName());
        TestStartDescriptor testStartDescriptor = new TestStartDescriptor(
                correlationData, description.getMethodName(), OffsetDateTime.now(),
                testClass, method, null
        );
        testRunTestIds.add(description.getDisplayName());

        registrar.registerTestStart(description.getDisplayName(), testStartDescriptor);
    }

    public void registerTestFinish(Description description) {
        if (testRunTestIds.contains(description.getDisplayName())) {
            OffsetDateTime endedAt = OffsetDateTime.now();
            String currentTestId = description.getDisplayName();
            TestFinishDescriptor testFinishDescriptor = new TestFinishDescriptor(Status.PASSED, endedAt);

            registrar.registerTestFinish(currentTestId, testFinishDescriptor);
        }
    }

    public void registerTestFailure(Failure failure) {
        OffsetDateTime endedAt = OffsetDateTime.now();
        String currentTestId = failure.getDescription().getDisplayName();
        TestFinishDescriptor result = new TestFinishDescriptor(Status.FAILED, endedAt, failure.getTrace());
        testRunTestIds.remove(currentTestId);

        registrar.registerTestFinish(currentTestId, result);
    }

    public void registerTestAssumptionFailure(Failure failure) {
        OffsetDateTime endedAt = OffsetDateTime.now();
        String currentTestId = failure.getDescription().getDisplayName();
        TestFinishDescriptor result = new TestFinishDescriptor(Status.SKIPPED, endedAt, failure.getTrace());
        testRunTestIds.remove(currentTestId);

        registrar.registerTestFinish(currentTestId, result);
    }

    public void registerTestIgnored(Description description) {
        OffsetDateTime endedAt = OffsetDateTime.now();
        String currentTestId = description.getDisplayName();
        TestFinishDescriptor result = new TestFinishDescriptor(Status.SKIPPED, endedAt);

        registrar.registerTestFinish(currentTestId, result);
    }

    private String buildCorrelationData(Description description) {
        Gson correlationData = new Gson();
        TestInvocationContext testInvocationContext = new TestInvocationContext(
                description.getDisplayName(), description.getClassName(), description.getMethodName()
        );
        return correlationData.toJson(testInvocationContext);
    }

}
