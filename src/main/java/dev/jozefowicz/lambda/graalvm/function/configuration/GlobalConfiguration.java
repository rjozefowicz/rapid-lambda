package dev.jozefowicz.lambda.graalvm.function.configuration;

import java.util.logging.Level;

import static java.util.Objects.nonNull;

public final class GlobalConfiguration {

    private static final GlobalConfiguration configuration = new GlobalConfiguration();
    private final Level logLevel;

    private GlobalConfiguration() {
        this.logLevel = detectLogLevel();
    }

    public Level getLogLevel() {
        return logLevel;
    }

    private Level detectLogLevel() {
        final String envLogLevel = System.getenv("RAPID_LAMBDA_LOG_LEVEL");
        if (nonNull(envLogLevel)) {
            try {
                return Level.parse(envLogLevel);
            } catch (Exception e) {
                // not important enough to crash function
                return Level.INFO;
            }
        } else {
            return Level.INFO;
        }
    }

    public static final GlobalConfiguration configuration() {
        return configuration;
    }

}
