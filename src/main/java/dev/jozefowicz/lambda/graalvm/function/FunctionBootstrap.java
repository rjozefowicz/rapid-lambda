package dev.jozefowicz.lambda.graalvm.function;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import dev.jozefowicz.lambda.graalvm.function.configuration.FunctionConfiguration;
import dev.jozefowicz.lambda.graalvm.function.configuration.GlobalModule;
import dev.jozefowicz.lambda.graalvm.runtime.GraalVMRuntime;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class FunctionBootstrap {

    private RequestHandler requestHandler;
    private Class<?> eventClass;
    private FunctionConfiguration functionConfiguration;

    private FunctionBootstrap(RequestHandler requestHandler, Class<?> eventClass, FunctionConfiguration functionConfiguration) {
        this.requestHandler = requestHandler;
        this.eventClass = eventClass;
        this.functionConfiguration = functionConfiguration;
    }

    public void bootstrap() {
        if (isNull(requestHandler) || isNull(eventClass)) {
            throw new IllegalStateException("Missing configuration. RequestHandler and Event class are required");
        }
        final List<Module> modules = new ArrayList<>();
        modules.add(new GlobalModule());
        if (nonNull(functionConfiguration)) {
            if (nonNull(functionConfiguration.getModule())) {
                modules.add(functionConfiguration.getModule());
            }
        }

        final Injector injector = Guice.createInjector(modules);
        final GraalVMRuntime graalVMRuntime = new GraalVMRuntime(requestHandler, eventClass);
        injector.injectMembers(graalVMRuntime);
        injector.injectMembers(requestHandler);
        graalVMRuntime.execute();
    }

    public static final FunctionBootstrap build(final RequestHandler requestHandler, final Class<?> eventClass) {
        return new FunctionBootstrap(requestHandler, eventClass, null);
    }

    public static final FunctionBootstrap build(final RequestHandler requestHandler, final Class<?> eventClass, final FunctionConfiguration functionConfiguration) {
        return new FunctionBootstrap(requestHandler, eventClass, functionConfiguration);
    }

}
