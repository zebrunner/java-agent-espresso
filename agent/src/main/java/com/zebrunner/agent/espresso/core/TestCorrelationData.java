package com.zebrunner.agent.espresso.core;

import com.google.gson.Gson;

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

    public String asJsonString() {
        return GSON.toJson(this);
    }

}
