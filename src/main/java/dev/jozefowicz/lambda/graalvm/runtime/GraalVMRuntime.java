package dev.jozefowicz.lambda.graalvm.runtime;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jozefowicz.lambda.graalvm.function.configuration.GlobalConfiguration;

import javax.inject.Inject;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;

public class GraalVMRuntime {

    private static final Logger LOGGER = Logger.getLogger(GraalVMRuntime.class.getName());

    private static final String AWS_LAMBDA_RUNTIME_API = System.getenv("AWS_LAMBDA_RUNTIME_API");
    private static final String LAMBDA_NEXT_INVOCATION_ENDPOINT = "http://" + AWS_LAMBDA_RUNTIME_API + "/2018-06-01/runtime/invocation/next";

    private final RequestHandler requestHandler;
    private final Class<?> eventClass;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private HttpClient httpClient;

    public GraalVMRuntime(RequestHandler requestHandler, Class<?> eventClass) {
        this.requestHandler = requestHandler;
        this.eventClass = eventClass;
    }

    public void execute() {
        LOGGER.log(GlobalConfiguration.configuration().getLogLevel(), "Bootstrap time: {0}",  ManagementFactory.getRuntimeMXBean().getUptime());
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        while (true) {
            try {
                final var startTimestamp = System.currentTimeMillis();
                final HttpRequest newInvocationRequest = HttpRequest.newBuilder().uri(new URI(LAMBDA_NEXT_INVOCATION_ENDPOINT)).build();
                final HttpResponse<String> invocationRequest = httpClient.send(newInvocationRequest, HttpResponse.BodyHandlers.ofString());
                String result = invokeHandler(invocationRequest);
                LOGGER.log(GlobalConfiguration.configuration().getLogLevel(), "Lambda result endpoint response: {0}", result);
                LOGGER.log(GlobalConfiguration.configuration().getLogLevel(), "Execution time: {0}", (System.currentTimeMillis() - startTimestamp));
            } catch (Exception e) {
                LOGGER.log(GlobalConfiguration.configuration().getLogLevel(), "Exception while executing handler logic: {0}", e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }

    private String invokeHandler(HttpResponse<String> invocationRequest) throws Exception {
        var invocationID = invocationRequest.headers().map().get("Lambda-Runtime-Aws-Request-Id").get(0);
        try {
            final Object input = this.objectMapper.readValue(invocationRequest.body(), eventClass);
            final Object result = requestHandler.handleRequest(input, new NoOpContext());
            final var invocationResultEndpoint = "http://" + AWS_LAMBDA_RUNTIME_API + "/2018-06-01/runtime/invocation/" + invocationID + "/response";
            final HttpRequest invocationResultRequest = HttpRequest
                    .newBuilder()
                    .uri(new URI(invocationResultEndpoint))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(result)))
                    .build();
            return httpClient.send(invocationResultRequest, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception e) {
            postError(invocationID, e.getLocalizedMessage());
            LOGGER.log(GlobalConfiguration.configuration().getLogLevel(), "Exception while executing handler logic: {0}", e.getLocalizedMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private void postError(final String invocationID, String message) {
        try {
            final var invocationResultEndpoint = "http://" + AWS_LAMBDA_RUNTIME_API + "/2018-06-01/runtime/invocation/" + invocationID + "/response";
            final HttpRequest invocationResultRequest = HttpRequest
                    .newBuilder()
                    .uri(new URI(invocationResultEndpoint))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(Map.of("errorMessage", message))))
                    .build();
            final HttpResponse<String> invocationResultResponse = httpClient.send(invocationResultRequest, HttpResponse.BodyHandlers.ofString());
            LOGGER.log(GlobalConfiguration.configuration().getLogLevel(), "Error endpoint result {0}", invocationResultResponse.body());
        } catch (Exception e) {
            LOGGER.log(GlobalConfiguration.configuration().getLogLevel(), "Unable to post invocation error {0}", e.getLocalizedMessage());
            throw new RuntimeException("Runtime execution exception");
        }
    }

}
