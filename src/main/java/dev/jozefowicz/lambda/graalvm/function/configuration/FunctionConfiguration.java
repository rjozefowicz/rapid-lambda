package dev.jozefowicz.lambda.graalvm.function.configuration;

import com.google.inject.Module;

public class FunctionConfiguration {

    private final Module module;

    private FunctionConfiguration(final Module module) {
        this.module = module;
    }

    public Module getModule() {
        return module;
    }

    public static final FunctionConfiguration newConfiguration(final Module module) {
        return new FunctionConfiguration(module);
    }
}
