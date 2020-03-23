package dev.jozefowicz.lambda.graalvm.function;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import dev.jozefowicz.lambda.graalvm.function.configuration.Configuration;
import dev.jozefowicz.lambda.graalvm.function.configuration.GlobalModule;
import dev.jozefowicz.lambda.graalvm.runtime.GraalVMRuntime;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class FunctionBootstrap {

    private RequestHandler requestHandler;
    private Class<?> eventClass;
    private Configuration configuration;

    private FunctionBootstrap(RequestHandler requestHandler, Class<?> eventClass, Configuration configuration) {
        this.requestHandler = requestHandler;
        this.eventClass = eventClass;
        this.configuration = configuration;
    }

    public void bootstrap() {
        if (isNull(requestHandler) || isNull(eventClass)) {
            throw new IllegalStateException("Missing configuration. RequestHandler and Event class are required");
        }
        final List<Module> modules = new ArrayList<>();
        modules.add(new GlobalModule());
        if (nonNull(configuration)) {
            if (nonNull(configuration.getModule())) {
                modules.add(configuration.getModule());
            }
        }

        final Injector injector = Guice.createInjector(modules);
        final GraalVMRuntime graalVMRuntime = new GraalVMRuntime(requestHandler, eventClass);
        injector.injectMembers(graalVMRuntime);
        graalVMRuntime.execute();
    }

    public static final FunctionBootstrap build(final RequestHandler requestHandler, final Class<?> eventClass) {
        return new FunctionBootstrap(requestHandler, eventClass, null);
    }

    public static final FunctionBootstrap build(final RequestHandler requestHandler, final Class<?> eventClass, final Configuration configuration) {
        return new FunctionBootstrap(requestHandler, eventClass, configuration);
    }

    public static void main(String[] args) {
        FunctionBootstrap.build((o, context) -> null, Object.class).bootstrap();
    }

}
