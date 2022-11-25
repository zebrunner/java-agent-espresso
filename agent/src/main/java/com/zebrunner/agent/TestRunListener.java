package com.zebrunner.agent;

import android.util.Log;

import com.zebrunner.agent.client.RetrofitZebrunnerApiClient;
import com.zebrunner.agent.core.registrar.ClientRegistrar;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class TestRunListener extends RunListener {

    private static final String TAG = "TestRunListener";
    private final AndroidJunitAdapter adapter;

    public TestRunListener() {
        ClientRegistrar.registry(RetrofitZebrunnerApiClient.getInstance());
        adapter = new AndroidJunitAdapter();
    }

    @Override
    public void testRunStarted(Description description) throws Exception {
        Log.d(TAG, "Test run started");
        adapter.registerRunStart(description);
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        Log.d(TAG, "Test run finished");
        adapter.registerRunFinish(result);
    }

    @Override
    public void testStarted(Description description) throws Exception {
        Log.d(TAG, "Test started");
        adapter.registerTestStart(description);
    }

    @Override
    public void testFinished(Description description) throws Exception {
        Log.d(TAG, "Test finished");
        adapter.registerTestFinish(description);
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        Log.e(TAG, "Test failure " + failure.getMessage());
        adapter.registerTestFailure(failure);
    }

    //TODO 2022-11-04 What is test assumption failure?
    @Override
    public void testAssumptionFailure(Failure failure) {
        Log.d(TAG, "Test assumption failure");
    }

    //TODO 2022-11-04 Check test ignored???
    @Override
    public void testIgnored(Description description) throws Exception {
        Log.d(TAG, "Test ignored");
    }

}
