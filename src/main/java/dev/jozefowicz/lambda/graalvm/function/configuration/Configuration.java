package dev.jozefowicz.lambda.graalvm.function.configuration;

import com.google.inject.Module;

public class Configuration {

    private final Module module;

    private Configuration(final Module module) {
        this.module = module;
    }

    public Module getModule() {
        return module;
    }

    public static final Configuration newConfiguration(final Module module) {
        return new Configuration(module);
    }
}
