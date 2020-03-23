package dev.jozefowicz.lambda.graalvm.function;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import dev.jozefowicz.lambda.graalvm.function.configuration.GlobalConfiguration;

import java.util.logging.Logger;

public class DefaultLambdaLogger implements LambdaLogger {

    private static final Logger LOGGER = Logger.getLogger(DefaultLambdaLogger.class.getName());

    @Override
    public void log(String message) {
        LOGGER.log(GlobalConfiguration.configuration().getLogLevel(), message);
    }

    @Override
    public void log(byte[] bytes) {
        LOGGER.log(GlobalConfiguration.configuration().getLogLevel(), new String(bytes));
    }
}
