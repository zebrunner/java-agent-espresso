package com.zebrunner.agent.model;

public class TestContext {

    private String displayName;
    private String testClassName;
    private String methodName;

    public TestContext() {}

    public TestContext(String displayName, String testClassName, String methodName) {
        this.displayName = displayName;
        this.testClassName = testClassName;
        this.methodName = methodName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getTestClassName() {
        return testClassName;
    }

    public void setTestClassName(String testClassName) {
        this.testClassName = testClassName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
}
