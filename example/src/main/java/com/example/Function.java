package com.example;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import dev.jozefowicz.lambda.graalvm.function.FunctionBootstrap;
import dev.jozefowicz.lambda.graalvm.function.configuration.FunctionConfiguration;

public class Function {

    public static void main(String[] args) {
        FunctionBootstrap.build(new MeasurementHandler(), APIGatewayProxyRequestEvent.class, FunctionConfiguration.newConfiguration(new Configuration())).bootstrap();
    }

}
