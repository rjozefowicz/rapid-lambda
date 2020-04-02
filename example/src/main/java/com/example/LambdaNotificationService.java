package com.example;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;

import javax.inject.Inject;
import java.nio.charset.Charset;

public class LambdaNotificationService implements NotificationService {

    private String functionName = System.getenv("NOTIFICATION_FUNCTION_NAME");

    @Inject
    private LambdaClient lambdaClient;

    @Override
    public void send(String serialNumber, String message) {
        final InvokeRequest invokeRequest = InvokeRequest
                .builder()
                .functionName(functionName)
                .payload(SdkBytes.fromString(message, Charset.forName("UTF-8")))
                .build();
        lambdaClient.invoke(invokeRequest);
    }
}
