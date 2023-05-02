package com.zebrunner.agent.espresso.core;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TestCorrelationData {

    private static final Gson GSON = new Gson();

    String thread = Thread.currentThread().getName();

    String className;
    String methodName;
    List<String> parameterClassNames;

    String displayName;

    @Override
    public String toString() {
        List<Object> buildParameters = new ArrayList<>(5);
        buildParameters.add(Thread.currentThread().getName());
        buildParameters.add(className);
        buildParameters.add(methodName);
        buildParameters.add(String.join(", ", parameterClassNames));

        return String.format("[%s]: %s.%s(%s)", buildParameters.toArray());
    }

    public String asJsonString() {
        return GSON.toJson(this);
    }

}
