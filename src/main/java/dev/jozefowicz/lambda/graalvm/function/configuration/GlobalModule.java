package dev.jozefowicz.lambda.graalvm.function.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.inject.AbstractModule;

import java.net.http.HttpClient;

public class GlobalModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ObjectMapper.class).toInstance(objectMapper());
        bind(HttpClient.class).toInstance(HttpClient.newHttpClient());
    }

    private ObjectMapper objectMapper() {
        final var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));
        return objectMapper;
    }
}
