package com.zebrunner.agent.espresso;

import com.zebrunner.agent.core.registrar.ClientRegistrar;
import com.zebrunner.agent.espresso.client.impl.ZebrunnerApiClientImpl;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestRunListener extends RunListener {

    private final JunitAdapter adapter;

    public TestRunListener() {
        ClientRegistrar.register(ZebrunnerApiClientImpl.getInstance());

        this.adapter = new JunitAdapter();
    }

    @Override
    public void testRunStarted(Description description) {
        log.debug("Registering test run start...");

        adapter.registerRunStart(description);

        log.debug("Registering test run start finished.");
    }

    @Override
    @SneakyThrows
    public void testRunFinished(Result result) {
        log.debug("Registering test run finish...");

        adapter.registerRunFinish(result);

        log.debug("Registering test run finish finished.");
    }

    @Override
    public void testStarted(Description description) {
        log.debug("Registering test start...");

        adapter.registerTestStart(description);

        log.debug("Registering test start finished.");
    }

    @Override
    public void testFinished(Description description) {
        log.debug("Registering test finish...");

        adapter.registerTestFinish(description);

        log.debug("Registering test finish finished.");
    }

    @Override
    public void testFailure(Failure failure) {
        log.debug("Registering test failure...");

        adapter.registerTestFailure(failure);

        log.debug("Registering test failure finished.");
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
        log.debug("Registering test assumption failure...");

        adapter.registerTestAssumptionFailure(failure);

        log.debug("Registering test assumption failure finished.");
    }

    @Override
    public void testIgnored(Description description) {
        log.debug("Registering test ignored...");

        adapter.registerTestIgnored(description);

        log.debug("Registering test ignored finished.");
    }

}
