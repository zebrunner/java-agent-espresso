package com.zebrunner.agent;

import android.util.Log;

import com.google.gson.Gson;
import com.zebrunner.agent.core.registrar.TestRunRegistrar;
import com.zebrunner.agent.core.registrar.descriptor.Status;
import com.zebrunner.agent.core.registrar.descriptor.TestFinishDescriptor;
import com.zebrunner.agent.core.registrar.descriptor.TestRunFinishDescriptor;
import com.zebrunner.agent.core.registrar.descriptor.TestRunStartDescriptor;
import com.zebrunner.agent.core.registrar.descriptor.TestStartDescriptor;
import com.zebrunner.agent.model.TestContext;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AndroidJunitAdapter {

    private static final String TAG = "AndroidJunitAdapter";
    private static final TestRunRegistrar registrar = TestRunRegistrar.getInstance();
    private static List<String> testsInExecution = Collections.synchronizedList(new ArrayList<>());

    public void registerRunStart(Description description) {
        Log.d(TAG, "Register run start");
        String name = description.getChildren().stream()
                .findFirst().orElseThrow(IllegalArgumentException::new)
                .getDisplayName();
        TestRunStartDescriptor testRunStartDescriptor = new TestRunStartDescriptor(
                name,
                "espresso",
                OffsetDateTime.now(),
                name
        );
        registrar.registerStart(testRunStartDescriptor);
    }

    public void registerRunFinish(Result result) {
        Log.d(TAG, "Register run finish");
        registrar.registerFinish(new TestRunFinishDescriptor(OffsetDateTime.now()));
    }

    public void registerTestStart(Description description) {
        Log.d(TAG, "Register test start");
        Class<?> klass = null;
        try {
            klass = Class.forName(description.getClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Method method = null;
        try {
            method = klass.getDeclaredMethod(description.getMethodName());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        String correlationData = buildCorrelationData(description);
        TestStartDescriptor testStartDescriptor = new TestStartDescriptor(
                correlationData,
                description.getDisplayName(),
                OffsetDateTime.now(),
                description.getTestClass(),
                method,
                null
        );

        testsInExecution.add(generateTestId(description));
        registrar.registerTestStart(generateTestId(description), testStartDescriptor);
    }

    public void registerTestFinish(Description description) {
        Log.d(TAG, "Register test finish");
        String currentTestId = generateTestId(description);
        if (testsInExecution.contains(currentTestId)) {
            OffsetDateTime endedAt = OffsetDateTime.now();
            TestFinishDescriptor testFinishDescriptor = new TestFinishDescriptor(Status.PASSED, endedAt);
            testsInExecution.remove(currentTestId);
            registrar.registerTestFinish(currentTestId, testFinishDescriptor);
        }
    }

    public void registerTestFailure(Failure failure) {
        Log.e(TAG, "Register test failure" + failure.getMessage());
        OffsetDateTime endedAt = OffsetDateTime.now();
        TestFinishDescriptor result = new TestFinishDescriptor(Status.FAILED, endedAt, failure.getTrace());
        String currentTestId = generateTestId(failure.getDescription());
        testsInExecution.remove(currentTestId);
        registrar.registerTestFinish(currentTestId, result);
    }

    private String buildCorrelationData(Description description) {
        TestContext testContext = new TestContext(
                description.getDisplayName(),
                description.getClassName(),
                description.getMethodName()
        );
        Gson gsonCorrelationData = new Gson();
        return gsonCorrelationData.toJson(testContext);
    }

    private String generateTestId(Description description) {
        return description.getDisplayName();
    }
}
